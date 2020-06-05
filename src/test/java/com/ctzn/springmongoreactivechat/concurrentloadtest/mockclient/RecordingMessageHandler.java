package com.ctzn.springmongoreactivechat.concurrentloadtest.mockclient;

import java.util.Collection;
import java.util.List;
import java.util.Map;

interface RecordingMessageHandler {
    void handleJsonMessage(String json);

    Collection<ChatClient> getChatClients();

    Map<String, List<ServerMessage>> getMessageMap();
}
