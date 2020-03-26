package com.ctzn.springmongoreactivechat.websocket;

import com.ctzn.springmongoreactivechat.domain.ChatEvent;
import com.ctzn.springmongoreactivechat.domain.Message;
import com.ctzn.springmongoreactivechat.repository.MessageRepository;
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

    private MessageRepository messageRepository;

    public MessageHandler(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    private static String getPathParam(WebSocketSession webSocketSession, String uriTemplate, String key) {
        String path = webSocketSession.getHandshakeInfo().getUri().getPath();
        UriTemplate template = new UriTemplate(uriTemplate);
        Map<String, String> parameters = template.match(path);
        return parameters.get(key);
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        Flux<String> g = messageRepository.findAllMessages().map(ChatEvent::fromMessage).map(ChatEvent::asJson);
        return session.send(g.map(session::textMessage))
                .and(session.receive().map(WebSocketMessage::getPayloadAsText)
                        .flatMap(x -> messageRepository.save(new Message("nick", x))));
    }
}
