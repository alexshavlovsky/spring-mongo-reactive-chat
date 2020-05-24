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

public class ReactiveTestClient extends AbstractTestClient implements WebSocketHandler {

    private final static WebSocketClient client = new ReactorNettyWebSocketClient();
    private final Disposable disposable;
    private final Flux<String> commands;

    ReactiveTestClient(Flux<String> commands) {
        this.commands = commands;
        disposable = client.execute(URI.create("ws://localhost:8085/ws/"), this).subscribe();
    }

    void close() {
        disposable.dispose();
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        Flux<WebSocketMessage> outgoingMessages = Flux.just(clientMessageFactory.getUpdateMe())
                .map(gson::toJson)
                .map(session::textMessage);

        Mono<Void> incomingMessages = session.receive()
                .map(WebSocketMessage::getPayloadAsText).mergeWith(commands)
                .doOnNext(this::handleServerMessage).then();

        return session.send(outgoingMessages).and(incomingMessages);
    }
}
