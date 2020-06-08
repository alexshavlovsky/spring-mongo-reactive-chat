package com.ctzn.springmongoreactivechat.concurrentloadtest.mockclient;

public interface MockChatClient extends ChatRecorder {
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
}
