package com.ctzn.springmongoreactivechat.websocket;

import com.ctzn.springmongoreactivechat.domain.DomainMapper;
import com.ctzn.springmongoreactivechat.domain.dto.IncomingMessage;
import com.ctzn.springmongoreactivechat.service.AttachmentHandlerService;
import com.ctzn.springmongoreactivechat.service.BroadcastEmitterService;
import com.ctzn.springmongoreactivechat.service.ChatBrokerService;
import com.ctzn.springmongoreactivechat.service.messages.BroadcastMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static com.ctzn.springmongoreactivechat.websocket.ClientStreamTransformers.*;

@Component
public class MessageHandler implements WebSocketHandler {

    @Value("${chat_client_greeting_timeout}")
    int GREETING_TIMEOUT;

    private final ChatBrokerService chatBroker;
    private final BroadcastMessageService broadcastMessageService;
    private final BroadcastEmitterService broadcastEmitterService;
    private final AttachmentHandlerService attachmentHandlerService;
    private final DomainMapper mapper;

    public MessageHandler(ChatBrokerService chatBroker, BroadcastMessageService broadcastMessageService, BroadcastEmitterService broadcastEmitterService, AttachmentHandlerService attachmentHandlerService, DomainMapper mapper) {
        this.chatBroker = chatBroker;
        this.broadcastMessageService = broadcastMessageService;
        this.broadcastEmitterService = broadcastEmitterService;
        this.attachmentHandlerService = attachmentHandlerService;
        this.mapper = mapper;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String sessionId = session.getId();
        Logger LOG = LoggerFactory.getLogger(MessageHandler.class.getName() + " [" + sessionId + "]");

        Flux<IncomingMessage> incoming = session.receive()
                .transform(parseJsonMessage(mapper, IncomingMessage.class))
                .publish().autoConnect(2);

        Mono<Void> input = incoming.transform(skipGreeting())
                .doOnError(e -> LOG.error(e.getMessage()))
                .transform(handleClientMessage(sessionId, chatBroker, broadcastMessageService, broadcastEmitterService, attachmentHandlerService, mapper, LOG))
                .then();

        Flux<String> outgoing = incoming.transform(parseGreetingTimeout(sessionId, GREETING_TIMEOUT))
                .flatMap(chatClient -> chatBroker.addClient(chatClient, LOG)
                        .concatWith(Flux.merge(
                                chatBroker.getTopic(),
                                broadcastMessageService.getTopic(),
                                broadcastEmitterService.getTopic()
                        ))
                        .map(mapper::toJson)
                        .doOnNext(json -> LOG.trace("==>{}", json))
                        .doFinally(sig -> chatBroker.removeClient(sessionId, LOG))
                );

        Mono<Void> output = session.send(outgoing.map(session::textMessage));

        return Mono.zip(input, output).then();
    }
}
