package com.ctzn.springmongoreactivechat.websocket;

import com.ctzn.springmongoreactivechat.domain.DomainMapper;
import com.ctzn.springmongoreactivechat.domain.Message;
import com.ctzn.springmongoreactivechat.domain.dto.ChatClient;
import com.ctzn.springmongoreactivechat.domain.dto.IncomingMessage;
import com.ctzn.springmongoreactivechat.service.BroadcastMessageService;
import com.ctzn.springmongoreactivechat.service.ChatBrokerService;
import com.ctzn.springmongoreactivechat.service.DirectBroadcastService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static com.ctzn.springmongoreactivechat.websocket.ClientStreamTransformers.parseGreetingAndTransform;
import static com.ctzn.springmongoreactivechat.websocket.ClientStreamTransformers.parseJsonMessage;

@Component
public class MessageHandler implements WebSocketHandler {

    private final BroadcastMessageService broadcastMessageService;
    private final DirectBroadcastService directBroadcastService;
    private final ChatBrokerService chatBroker;
    private final DomainMapper mapper;

    public MessageHandler(BroadcastMessageService broadcastMessageService, DirectBroadcastService directBroadcastService, ChatBrokerService chatBroker, DomainMapper mapper) {
        this.broadcastMessageService = broadcastMessageService;
        this.directBroadcastService = directBroadcastService;
        this.chatBroker = chatBroker;
        this.mapper = mapper;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String sessionId = session.getId();
        Logger LOG = LoggerFactory.getLogger(MessageHandler.class.getName() + " [" + sessionId + "]");

        Flux<IncomingMessage> incoming = session.receive()
                .transform(parseJsonMessage(mapper, IncomingMessage.class))
                .publish().autoConnect(2);

        Mono<Void> input = incoming.transform(parseGreetingAndTransform(f -> f.skip(1)))
                .doOnError(e -> LOG.error(e.getMessage()))
                .flatMap(userMessage -> {
                    switch (userMessage.getType()) {
                        case "updateMe":
                            ChatClient previous = chatBroker.updateClient(userMessage.getUser().toChatClient(sessionId), LOG);
                            directBroadcastService.send(Message.newInfo(String.format(
                                    "(%s) has changed nick to (%s)", previous.getNick(), userMessage.getUser().getNick()
                            )));
                            break;
                        case "setTyping":
                            LOG.trace("...{}", userMessage);
                            directBroadcastService.send(userMessage.toMessage(sessionId));
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
                })
                .then();

        Flux<String> source = incoming.transform(parseGreetingAndTransform(f -> f)).next().take(Duration.ofSeconds(5))
                .flatMapMany(message ->
                        chatBroker.addClient(message.getUser().toChatClient(sessionId), LOG)
                                .concatWith(Flux.merge(
                                        chatBroker.getTopic(),
                                        broadcastMessageService.getTopic(),
                                        directBroadcastService.getTopic()
                                ))
                                .map(mapper::toJson)
                                .doOnNext(json -> LOG.trace("==>{}", json))
                                .doFinally(sig -> chatBroker.removeClient(sessionId, LOG))
                );

        Mono<Void> output = session.send(source.map(session::textMessage));

        return Mono.zip(input, output).then();
    }
}
