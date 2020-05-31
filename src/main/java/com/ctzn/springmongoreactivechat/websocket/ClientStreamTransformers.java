package com.ctzn.springmongoreactivechat.websocket;

import com.ctzn.springmongoreactivechat.domain.ChatClient;
import com.ctzn.springmongoreactivechat.domain.DomainMapper;
import com.ctzn.springmongoreactivechat.domain.IncomingMessage;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.slf4j.Logger;
import org.springframework.web.reactive.socket.WebSocketMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

class ClientStreamTransformers {
    static Function<Flux<WebSocketMessage>, Publisher<IncomingMessage>> parseJsonMessage(DomainMapper mapper, Logger LOG) {
        return in -> in
                .map(WebSocketMessage::getPayloadAsText)
                .flatMap(json -> {
                    try {
                        return Mono.just(mapper.fromJson(json, IncomingMessage.class));
                    } catch (Exception e) {
                        LOG.error("Error parsing json: {} - {}", json, e.getMessage());
                        return Mono.error(e);
                    }
                });
    }

    static Function<Flux<IncomingMessage>, Publisher<IncomingMessage>> parseGreeting(String sessionId, Subscriber<ChatClient> clientGreeting, Logger LOG) {
        return in -> in
                .switchOnFirst((signal, flux) -> {
                    if (signal.hasValue()) {
                        IncomingMessage message = signal.get();
                        if (message != null && "hello".equals(message.getType())) {
                            clientGreeting.onNext(ChatClient.fromMessage(sessionId, message));
                            return flux.skip(1);
                        }
                    }
                    Exception e = new UnsupportedOperationException("Malformed greeting: " + signal);
                    LOG.error(e.getMessage());
                    return Mono.error(e);
                });
    }
}
