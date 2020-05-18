package com.ctzn.springmongoreactivechat.service;

import com.ctzn.springmongoreactivechat.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ReplayProcessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatBrokerService {

    private Logger LOG = LoggerFactory.getLogger(ChatBrokerService.class);

    private ReplayProcessor<Message> broadcastTopic = ReplayProcessor.create(10);

    private Map<String, ChatClient> clients = new HashMap<>();
    private int snapshotVersion = 0;

    private DomainMapper mapper;

    public ChatBrokerService(DomainMapper mapper) {
        this.mapper = mapper;
    }

    synchronized public Mono<Message> addClient(String sessionId) {
        ChatClient thisClient = ChatClient.newInstance(sessionId);
        ChatSnapshot snapshot = new ChatSnapshot(++snapshotVersion, getClientsList(), thisClient);

        clients.put(sessionId, thisClient);
        LOG.info(" + [{}] (total clients: {})", sessionId, clients.size());

        broadcastTopic.onNext(
                Message.newObject("snapshotUpdate", mapper.asJson(
                        new ChatSnapshotUpdate(snapshotVersion, "addUser", thisClient)))
        );
        return Mono.just(Message.newObject("snapshot", mapper.asJson(snapshot)));
    }

    synchronized public void removeClient(String sessionId) {
        ChatClient thisClient = clients.get(sessionId);
        clients.remove(sessionId);
        LOG.info(" x [{}] (total clients: {})", sessionId, clients.size());
        broadcastTopic.onNext(
                Message.newObject("snapshotUpdate", mapper.asJson(
                        new ChatSnapshotUpdate(snapshotVersion, "removeUser", thisClient)))
        );
    }

    synchronized public void updateClient(String sessionId, IncomingMessage message) {
        ChatClient thisClient = clients.get(sessionId);
        thisClient.setClientId(message.getClientId());
        thisClient.setNick(message.getUserNick());
        broadcastTopic.onNext(
                Message.newObject("snapshotUpdate", mapper.asJson(
                        new ChatSnapshotUpdate(snapshotVersion, "updateUser", thisClient)))
        );
    }

    private List<ChatClient> getClientsList() {
        return new ArrayList<>(clients.values());
    }

    public ReplayProcessor<Message> getBroadcastTopic() {
        return broadcastTopic;
    }
}
