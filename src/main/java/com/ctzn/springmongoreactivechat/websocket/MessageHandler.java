package com.ctzn.springmongoreactivechat.websocket;

import com.ctzn.springmongoreactivechat.domain.DomainMapper;
import com.ctzn.springmongoreactivechat.domain.IncomingMessage;
import com.ctzn.springmongoreactivechat.domain.Message;
import com.ctzn.springmongoreactivechat.service.ChatBrokerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.util.UriTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class MessageHandler implements WebSocketHandler {

    private Logger LOG = LoggerFactory.getLogger(MessageHandler.class);

    private ReactiveMongoOperations mongo;
    private ChatBrokerService chatBroker;
    private DomainMapper mapper;

    public MessageHandler(ReactiveMongoOperations mongo, ChatBrokerService chatBroker, DomainMapper mapper) {
        this.mongo = mongo;
        this.chatBroker = chatBroker;
        this.mapper = mapper;
    }

    private static String getPathParam(WebSocketSession webSocketSession, String uriTemplate, String key) {
        String path = webSocketSession.getHandshakeInfo().getUri().getPath();
        UriTemplate template = new UriTemplate(uriTemplate);
        Map<String, String> parameters = template.match(path);
        return parameters.get(key);
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String sessionId = session.getId();

        Flux<WebSocketMessage> messageUpdates = chatBroker.addClient(session)
                .concatWith(chatBroker.getBroadcastTopic().mergeWith(mongo.tail(new BasicQuery("{}"), Message.class)))
                .map(mapper::asJson)
                .doOnNext(json -> LOG.trace("==>[{}] {}", sessionId, json))
                .map(session::textMessage);

        Flux<Message> incomingMessages = session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .doOnNext(rawText -> LOG.info("<--[{}] {}", sessionId, rawText))
                .map(rawText -> mapper.fromJson(rawText, IncomingMessage.class))
                .map(message -> Message.newText(session, message))
                .flatMap(mongo::save)
                .doFinally(sig -> chatBroker.removeClient(session));

        return session.send(messageUpdates).and(incomingMessages);
    }

}
