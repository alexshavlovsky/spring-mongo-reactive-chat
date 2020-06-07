package com.ctzn.springmongoreactivechat.websocket;

import com.ctzn.springmongoreactivechat.domain.DomainMapper;
import com.ctzn.springmongoreactivechat.domain.dto.IncomingMessage;
import org.reactivestreams.Publisher;
import org.springframework.web.reactive.socket.WebSocketMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

class ClientStreamTransformers {
    static <T> Function<Flux<WebSocketMessage>, Publisher<T>> parseJsonMessage(DomainMapper mapper, Class<T> clazz) {
        return in -> in
                .map(WebSocketMessage::getPayloadAsText)
                .flatMap(json -> {
                    try {
                        return Mono.just(mapper.fromJson(json, clazz));
                    } catch (Exception e) {
                        return Mono.error(e);
                    }
                });
    }

    static <T> Function<Flux<IncomingMessage>, Publisher<T>> parseGreetingAndTransform(Function<Flux<IncomingMessage>, Publisher<T>> transformer) {
        return in -> in
                .switchOnFirst((signal, flux) -> {
                    if (signal.hasValue()) {
                        IncomingMessage message = signal.get();
                        if (!(message != null && "hello".equals(message.getType()))) {
                            Exception e = new UnsupportedOperationException("Malformed greeting: " + signal);
                            return flux.flatMap(m -> Mono.error(e));
                        }
                    }
                    return flux.transform(transformer);
                });
    }
}
