package com.ctzn.springmongoreactivechat.concurrentloadtest;

import com.ctzn.springmongoreactivechat.concurrentloadtest.mockclient.MockChatClient;
import com.ctzn.springmongoreactivechat.concurrentloadtest.mockclient.MockChatClientImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

class ReactorTestClient implements TestClient {


    private static AtomicInteger counter = new AtomicInteger(0);
    private final int id = counter.getAndIncrement();

    private Logger LOG = LoggerFactory.getLogger(ReactorTestClient.class + Integer.toString(id));

    private final static ConnectionProvider connectionProvider = ConnectionProvider.builder("ReactorTestClientConnectionProvider")
            .maxConnections(500)
            .pendingAcquireMaxCount(500)
            .pendingAcquireTimeout(Duration.ofSeconds(5))
            .build();
    private final static WebSocketClient client = new ReactorNettyWebSocketClient(HttpClient.create(connectionProvider));

    private final URI uri;
    private final WebSocketHandler handler;
    private Disposable disposable;

    private final Sinks.Many<String> unicastSink = Sinks.many().unicast().onBackpressureBuffer();

    private final MockChatClientImpl chat = new MockChatClientImpl(s -> {
        Sinks.EmitResult result = unicastSink.tryEmitNext(s);
        if (result != Sinks.EmitResult.OK) LOG.error("Emmit result: {}", result);
    });
    private final CountDownLatch disconnected = new CountDownLatch(1);

    private ReactorTestClient(URI uri, boolean autoGreeting) {
        this.uri = uri;
        Flux<String> unicastFlux = unicastSink.asFlux();
        Flux<String> output = autoGreeting ? Flux.just(chat.getHello()).concatWith(unicastFlux) : unicastFlux;
        handler = session -> Mono.zip(
                session.receive().map(WebSocketMessage::getPayloadAsText).doOnNext(chat::handleJson).then(),
                session.send(output.map(session::textMessage))
        ).doFinally(sig -> disconnected.countDown()).then();
    }

    static ReactorTestClient newInstance(String uri, boolean autoGreeting) {
        return new ReactorTestClient(URI.create(uri), autoGreeting);
    }

    @Override
    public void connect() {
        disposable = client.execute(uri, handler).doOnError(e -> System.out.println(id + ": " + e)).subscribe();
    }

    @Override
    public void close() {
        disposable.dispose();
    }

    @Override
    public MockChatClient getChat() {
        return chat;
    }

    @Override
    public boolean disconnected() {
        return disconnected.getCount() == 0;
    }
}
