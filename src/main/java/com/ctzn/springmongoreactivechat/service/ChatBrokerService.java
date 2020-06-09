package com.ctzn.springmongoreactivechat.service;

import com.ctzn.springmongoreactivechat.domain.DomainMapper;
import com.ctzn.springmongoreactivechat.domain.Message;
import com.ctzn.springmongoreactivechat.domain.dto.ChatClient;
import com.ctzn.springmongoreactivechat.domain.dto.ChatSnapshot;
import com.ctzn.springmongoreactivechat.domain.dto.ChatSnapshotUpdate;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import reactor.core.publisher.FluxSink;
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

    private final ReplayProcessor<Message> processor = ReplayProcessor.create(20);
    private final FluxSink<Message> sink = processor.sink(FluxSink.OverflowStrategy.BUFFER);
    private final Map<String, ChatClient> clients = new HashMap<>();
    private int snapshotVersion = 0;

    public ReplayProcessor<Message> getTopic() {
        return processor;
    }

    synchronized public Mono<Message> addClient(ChatClient client, Logger log) {
        broadcast("addUser", client);

        clients.put(client.getSessionId(), client);

        log.info(" + (total: {}) {}", clients.size(), client);

        return Mono.just(mapper.toMessage(new ChatSnapshot(++snapshotVersion, new ArrayList<>(clients.values()), client)));
    }

    synchronized public ChatClient updateClient(ChatClient client, Logger log) {
        broadcast("updateUser", client);

        ChatClient previous = clients.put(client.getSessionId(), client);

        log.info(" u (total: {}) {} from {}", clients.size(), client, previous);

        return previous;
    }

    synchronized public void removeClient(String sessionId, Logger log) {
        ChatClient client = clients.get(sessionId);

        broadcast("removeUser", client);

        clients.remove(sessionId);

        log.info(" x (total: {}) {}", clients.size(), client);
    }

    private void broadcast(String type, ChatClient client) {
        sink.next(mapper.toMessage(new ChatSnapshotUpdate(snapshotVersion, type, client)));
    }
}
