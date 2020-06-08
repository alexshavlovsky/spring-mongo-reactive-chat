package com.ctzn.springmongoreactivechat.concurrentloadtest.mockclient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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
    public Collection<ServerMessage> getServerMessages() {
        return serverMessages;
    }
}
