package com.ctzn.springmongoreactivechat.concurrentloadtest;

import com.ctzn.springmongoreactivechat.concurrentloadtest.mockclient.MockChatClient;
import com.ctzn.springmongoreactivechat.concurrentloadtest.mockclient.MockChatClientImpl;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.ReplayProcessor;

import java.net.URI;

class ReactorTestClient implements TestClient {

    private final URI uri;
    private final WebSocketHandler handler;
    private final static WebSocketClient client = new ReactorNettyWebSocketClient();
    private Disposable disposable;
    private final ReplayProcessor<String> out = ReplayProcessor.create();
    private final MockChatClientImpl chat = new MockChatClientImpl(out::onNext);

    private ReactorTestClient(URI uri, boolean autoGreeting) {
        this.uri = uri;
        Flux<String> greeting = autoGreeting ? Flux.just(chat.getHello()) : Flux.empty();
        handler = session -> session.send(greeting.concatWith(out).map(session::textMessage)).and(
                session.receive().map(WebSocketMessage::getPayloadAsText)
                        .doOnNext(chat::handleJson).then()
        );
    }

    static ReactorTestClient newInstance(String uri, boolean autoGreeting) {
        return new ReactorTestClient(URI.create(uri), autoGreeting);
    }

    @Override
    public void connect() {
        disposable = client.execute(uri, handler).subscribe();
    }

    @Override
    public void close() {
        disposable.dispose();
    }

    @Override
    public MockChatClient getChat() {
        return chat;
    }
}
