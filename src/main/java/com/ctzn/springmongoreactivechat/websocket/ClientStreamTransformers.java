package com.ctzn.springmongoreactivechat.websocket;

import com.ctzn.springmongoreactivechat.domain.DomainMapper;
import com.ctzn.springmongoreactivechat.domain.Message;
import com.ctzn.springmongoreactivechat.domain.dto.ChatClient;
import com.ctzn.springmongoreactivechat.domain.dto.IncomingMessage;
import com.ctzn.springmongoreactivechat.service.BroadcastEmitterService;
import com.ctzn.springmongoreactivechat.service.messages.BroadcastMessageService;
import com.ctzn.springmongoreactivechat.service.ChatBrokerService;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.springframework.web.reactive.socket.WebSocketMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
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


    static Function<Flux<IncomingMessage>, Publisher<IncomingMessage>> skipGreeting() {
        return in -> in.transform(parseGreetingAndTransform(f -> f.skip(1)));
    }

    static Function<Flux<IncomingMessage>, Publisher<ChatClient>> parseGreetingTimeout(String sessionId, int timeout) {
        return in -> in.transform(parseGreetingAndTransform(f -> f)).next().take(Duration.ofMillis(timeout))
                .map(greeting -> greeting.getUser().toChatClient(sessionId));
    }

    static Function<Flux<IncomingMessage>, Publisher<Message>> handleClientMessage(
            String sessionId, ChatBrokerService chatBroker, BroadcastMessageService broadcastMessageService, BroadcastEmitterService broadcastEmitterService, Logger LOG
    ) {
        return in -> in.
                flatMap(userMessage -> {
                    switch (userMessage.getType()) {
                        case "updateMe":
                            ChatClient previous = chatBroker.updateClient(userMessage.getUser().toChatClient(sessionId), LOG);
                            broadcastEmitterService.send(Message.newInfo(String.format(
                                    "(%s) has changed nick to (%s)", previous.getNick(), userMessage.getUser().getNick()
                            )));
                            break;
                        case "setTyping":
                            LOG.trace("...{}", userMessage);
                            broadcastEmitterService.send(userMessage.toMessage(sessionId));
                            break;
                        case "msg":
                        case "richMsg":
                            LOG.info("<--{}", userMessage);
                            return broadcastMessageService.saveMessage(userMessage.toMessage(sessionId));
                        default:
                            Exception e = new UnsupportedOperationException("Message type is not supported: " + userMessage);
                            LOG.error(e.getMessage());
                            return Mono.error(e);
                    }
                    return Mono.empty();
                });
    }
}
