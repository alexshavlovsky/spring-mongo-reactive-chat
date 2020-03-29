package com.ctzn.springmongoreactivechat.service;

import com.ctzn.springmongoreactivechat.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ReplayProcessor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChatBrokerService {

    private Logger LOG = LoggerFactory.getLogger(ChatBrokerService.class);

    private ReplayProcessor<Message> broadcastTopic = ReplayProcessor.create(10);

    private Map<String, WebSocketSession> sessions = new HashMap<>();
    private int snapshotVersion = 0;

    private DomainMapper mapper;

    public ChatBrokerService(DomainMapper mapper) {
        this.mapper = mapper;
    }

    synchronized public Mono<Message> addClient(WebSocketSession session) {
        ChatClient thisClient = ChatClient.fromSession(session);
        ChatSnapshot snapshot = new ChatSnapshot(++snapshotVersion, getClientsList(), thisClient);

        sessions.put(session.getId(), session);
        LOG.info(" + [{}] (total clients: {})", session.getId(), sessions.size());

        broadcastTopic.onNext(
                Message.newObject("snapshotUpdate", mapper.asJson(
                        new ChatSnapshotUpdate(snapshotVersion, "addUser", thisClient)))
        );
        return Mono.just(Message.newObject("snapshot", mapper.asJson(snapshot)));
    }

    synchronized public void removeClient(WebSocketSession session) {
        sessions.remove(session.getId());
        LOG.info(" x [{}] (total clients: {})", session.getId(), sessions.size());
        ChatClient thisClient = ChatClient.fromSession(session);
        broadcastTopic.onNext(
                Message.newObject("snapshotUpdate", mapper.asJson(
                        new ChatSnapshotUpdate(snapshotVersion, "removeUser", thisClient)))
        );
    }

    private List<ChatClient> getClientsList() {
        return sessions.values().stream()
                .map(ChatClient::fromSession)
                .collect(Collectors.toList());
    }

    public ReplayProcessor<Message> getBroadcastTopic() {
        return broadcastTopic;
    }
}
