package com.ctzn.springmongoreactivechat.concurrentloadtest.mockclient;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface MockChatClient {
    User getUser();

    void sendHello();

    void sendMsg(String text);

    void sendUpdateMe();

    void sendSetTyping();

    String getHello();

    String getMsg(String text);

    String getUpdateMe();

    String getSetTyping();

    void handleJson(String json);

    Map<String, List<ServerMessage>> getMessageMap();

    Collection<ChatClient> getChatClients();
}
