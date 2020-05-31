package com.ctzn.springmongoreactivechat.reactiveloadtest;

import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;

abstract class ReactiveTestClient extends AbstractTestClient implements WebSocketHandler {

    private final static WebSocketClient client = new ReactorNettyWebSocketClient();
    private final Disposable disposable;
    private final Flux<ClientMessage> out;

    ReactiveTestClient(String command, int delay) {
        super();
        Flux<ClientMessage> hello = Flux.just(clientMessageFactory.getHello());
        this.out = command == null ? hello : hello
                .concatWith(Flux.just(clientMessageFactory.getMsg(command)).delaySequence(Duration.ofMillis(delay)));
        disposable = open(this);
    }

    private Disposable open(WebSocketHandler handler) {
        return client.execute(URI.create("ws://localhost:8085/ws/"), handler).subscribe();
    }

    void close() {
        disposable.dispose();
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        Flux<WebSocketMessage> outgoingMessages = out
                .map(gson::toJson)
                .map(session::textMessage);

        Mono<Void> incomingMessages = session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .map(json -> gson.fromJson(json, ServerMessage.class))
                .doOnNext(this::handleServerMessage).then();

        return session.send(outgoingMessages).and(incomingMessages);
    }
}
