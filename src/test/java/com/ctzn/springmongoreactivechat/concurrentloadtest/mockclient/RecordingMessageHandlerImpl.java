package com.ctzn.springmongoreactivechat.concurrentloadtest.mockclient;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class RecordingMessageHandlerImpl implements RecordingMessageHandler {
    private final Mapper mapper;
    private final ChatSnapshotHandler snapshotHolder;
    private final List<ServerMessage> serverMessages = Collections.synchronizedList(new ArrayList<>());

    RecordingMessageHandlerImpl(Mapper mapper, ChatSnapshotHandler snapshotHolder) {
        this.mapper = mapper;
        this.snapshotHolder = snapshotHolder;
    }

    @Override
    public void handleJsonMessage(String json) {
        ServerMessage message = mapper.fromJson(json, ServerMessage.class);
        switch (message.getType()) {
            case "snapshot":
                snapshotHolder.handleSnapshot(mapper.fromJson(message.getPayload(), ChatSnapshot.class));
                break;
            case "snapshotUpdate":
                snapshotHolder.handleSnapshotUpdate(mapper.fromJson(message.getPayload(), ChatSnapshotUpdate.class));
                break;
        }
        serverMessages.add(message);
    }

    @Override
    public Collection<ChatClient> getChatClients() {
        return snapshotHolder.getClients();
    }

    @Override
    public Map<String, List<ServerMessage>> getMessageMap() {
        return serverMessages.stream()
                .collect(HashMap::new, (m, v) -> m.merge(v.getType(), Stream.of(v).collect(Collectors.toList()), (a, n) -> {
                    a.addAll(n);
                    return a;
                }), Map::putAll);
    }
}
