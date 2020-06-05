package com.ctzn.springmongoreactivechat.concurrentloadtest.mockclient;

class ClientMessageFactory {
    private int frameId = 0;
    private final User user;

    ClientMessageFactory(User user) {
        this.user = user;
    }

    private ClientMessage getTypedMessage(String type, String payload) {
        return new ClientMessage(frameId++, user, type, payload);
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
