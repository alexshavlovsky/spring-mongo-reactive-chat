package com.ctzn.springmongoreactivechat.service;

import com.ctzn.springmongoreactivechat.domain.*;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ReplayProcessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Service
public class ChatBrokerService {

    private final DomainMapper mapper;

    public ChatBrokerService(DomainMapper mapper) {
        this.mapper = mapper;
    }

    private final ReplayProcessor<Message> broadcastTopic = ReplayProcessor.create(10);
    private final Map<String, ChatClient> clients = new HashMap<>();
    private int snapshotVersion = 0;

    public ReplayProcessor<Message> getBroadcastTopic() {
        return broadcastTopic;
    }

    synchronized public Mono<Message> addClient(String sessionId, ChatClient client, Logger log) {
        broadcast("addUser", client);

        clients.put(sessionId, client);

        log.info(" + (total: {}) {}", clients.size(), client);

        return Mono.just(Message.newObject("snapshot", mapper.asJson(new ChatSnapshot(++snapshotVersion, new ArrayList<>(clients.values()), client))));
    }

    synchronized public void updateClient(String sessionId, IncomingMessage message, Logger log) {
        ChatClient client = ChatClient.fromMessage(sessionId, message);
        broadcast("updateUser", client);

        ChatClient previous = clients.put(sessionId, client);

        log.info(" u (total: {}) {} from {}", clients.size(), client, previous);
    }

    synchronized public void removeClient(String sessionId, Logger log) {
        ChatClient client = clients.get(sessionId);
        broadcast("removeUser", client);

        clients.remove(sessionId);

        log.info(" x (total: {}) {}", clients.size(), client);
    }

    private void broadcast(String type, ChatClient client) {
        broadcastTopic.onNext(
                Message.newObject("snapshotUpdate", mapper.asJson(new ChatSnapshotUpdate(snapshotVersion, type, client)))
        );
    }
}
