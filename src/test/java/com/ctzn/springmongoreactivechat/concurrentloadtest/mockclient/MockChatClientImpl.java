package com.ctzn.springmongoreactivechat.concurrentloadtest.mockclient;

import java.util.Collection;
import java.util.UUID;
import java.util.function.Consumer;

public class MockChatClientImpl implements MockChatClient {
    private final Mapper mapper = new Mapper();
    private final RecordingMessageHandler messageHandler;
    private final User user;
    private final ClientMessageFactory messageFactory;
    private final Consumer<String> jsonConsumer;

    public MockChatClientImpl(User user, Consumer<String> jsonConsumer) {
        this.jsonConsumer = jsonConsumer;
        messageHandler = new RecordingMessageHandlerImpl(mapper, new ChatSnapshotHandlerImpl());
        this.user = user;
        messageFactory = new ClientMessageFactory(user);
    }

    public MockChatClientImpl(Consumer<String> jsonConsumer) {
        this(new User(UUID.randomUUID().toString(), UUID.randomUUID().toString()), jsonConsumer);
    }

    @Override
    public User getUser() {
        return user;
    }

    @Override
    public void sendHello() {
        sendMessage(getHello());
    }

    @Override
    public void sendMsg(String text) {
        sendMessage(getMsg(text));
    }

    @Override
    public void sendUpdateMe() {
        sendMessage(getUpdateMe());
    }

    @Override
    public void sendSetTyping() {
        sendMessage(getSetTyping());
    }

    @Override
    public String getHello() {
        return mapper.toJson(messageFactory.getHello());
    }

    @Override
    public String getMsg(String text) {
        return mapper.toJson(messageFactory.getMsg(text));
    }

    @Override
    public String getUpdateMe() {
        return mapper.toJson(messageFactory.getUpdateMe());
    }

    @Override
    public String getSetTyping() {
        return mapper.toJson(messageFactory.getSetTyping());
    }

    @Override
    public void handleJson(String json) {
        messageHandler.handleJsonMessage(json);
    }

    @Override
    public Collection<ServerMessage> getServerMessages() {
        return messageHandler.getServerMessages();
    }

    @Override
    public Collection<ChatClient> getChatClients() {
        return messageHandler.getChatClients();
    }

    private void sendMessage(String json) {
        jsonConsumer.accept(json);
    }
}
