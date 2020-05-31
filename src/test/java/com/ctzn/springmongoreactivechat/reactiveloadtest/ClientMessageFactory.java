package com.ctzn.springmongoreactivechat.reactiveloadtest;

class ClientMessageFactory {
    private int frameId = 0;
    private final ChatClient chatClient;

    ClientMessageFactory(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    private ClientMessage getTypedMessage(String type, String payload) {
        return new ClientMessage(frameId++, chatClient.getClientId(), chatClient.getNick(), type, payload);
    }

    ClientMessage getHello() {
        return getTypedMessage("hello", "");
    }

    ClientMessage getUpdateMe() {
        return getTypedMessage("updateMe", "");
    }

    ClientMessage getSetTyping() {
        return getTypedMessage("setTyping", "");
    }

    ClientMessage getMsg(String text) {
        return getTypedMessage("msg", text);
    }
}
