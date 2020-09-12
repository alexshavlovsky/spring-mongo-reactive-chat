package com.ctzn.springmongoreactivechat.concurrentloadtest;

import com.ctzn.springmongoreactivechat.concurrentloadtest.mockclient.MockChatClient;
import com.ctzn.springmongoreactivechat.concurrentloadtest.mockclient.MockChatClientImpl;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.UnicastProcessor;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

class ReactorTestClient implements TestClient {

    private static AtomicInteger counter = new AtomicInteger(0);
    private final int id = counter.getAndIncrement();

    private final static ConnectionProvider connectionProvider = ConnectionProvider.builder("ReactorTestClientConnectionProvider")
            .maxConnections(500)
            .pendingAcquireMaxCount(500)
            .pendingAcquireTimeout(Duration.ofSeconds(5))
            .build();
    private final static WebSocketClient client = new ReactorNettyWebSocketClient(HttpClient.create(connectionProvider));

    private final URI uri;
    private final WebSocketHandler handler;
    private Disposable disposable;
    private final UnicastProcessor<String> processor = UnicastProcessor.create();
    private final FluxSink<String> sink = processor.sink(FluxSink.OverflowStrategy.BUFFER);
    private final MockChatClientImpl chat = new MockChatClientImpl(sink::next);
    private final CountDownLatch disconnected = new CountDownLatch(1);

    private ReactorTestClient(URI uri, boolean autoGreeting) {
        this.uri = uri;
        Flux<String> output = autoGreeting ? Flux.just(chat.getHello()).concatWith(processor) : processor;
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
