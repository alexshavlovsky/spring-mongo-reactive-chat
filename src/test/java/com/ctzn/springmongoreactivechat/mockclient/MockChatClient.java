package com.ctzn.springmongoreactivechat.mockclient;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface MockChatClient {
    User getUser();

    void sendHello();

    void sendMsg(String text);

    void sendUpdateMe();

    void sendSetTyping();

    void handleJson(String json);

    Map<String, List<ServerMessage>> getMessageMap();

    Collection<ChatClient> getChatClients();
}
