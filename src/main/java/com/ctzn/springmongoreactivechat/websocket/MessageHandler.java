package com.ctzn.springmongoreactivechat.websocket;

import com.ctzn.springmongoreactivechat.domain.DomainMapper;
import com.ctzn.springmongoreactivechat.domain.IncomingMessage;
import com.ctzn.springmongoreactivechat.domain.Message;
import com.ctzn.springmongoreactivechat.service.BroadcastMessageService;
import com.ctzn.springmongoreactivechat.service.ChatBrokerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class MessageHandler implements WebSocketHandler {

    private Logger LOG = LoggerFactory.getLogger(MessageHandler.class);

    private BroadcastMessageService broadcastMessageService;
    private ChatBrokerService chatBroker;
    private DomainMapper mapper;

    public MessageHandler(BroadcastMessageService broadcastMessageService, ChatBrokerService chatBroker, DomainMapper mapper) {
        this.broadcastMessageService = broadcastMessageService;
        this.chatBroker = chatBroker;
        this.mapper = mapper;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String sessionId = session.getId();

        Flux<WebSocketMessage> outgoingMessages = chatBroker.addClient(sessionId)
                .concatWith(chatBroker.getBroadcastTopic().mergeWith(broadcastMessageService.getTopic()))
                .map(mapper::asJson)
                .doOnNext(json -> LOG.trace("==>[{}] {}", sessionId, json))
                .map(session::textMessage);

        Flux<Message> incomingMessages = session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .doOnNext(rawText -> LOG.info("<--[{}] {}", sessionId, rawText))
                .map(rawText -> mapper.fromJson(rawText, IncomingMessage.class))
                .doOnNext(message -> {
                    if ("updateMe".equals(message.getType())) chatBroker.updateClient(sessionId, message);
                })
                .filter(message -> "msg".equals(message.getType()))
                .map(message -> Message.newText(session, message))
                .flatMap(broadcastMessageService::saveMessage)
                .doFinally(sig -> chatBroker.removeClient(sessionId));

        return session.send(outgoingMessages).and(incomingMessages);
    }

}
