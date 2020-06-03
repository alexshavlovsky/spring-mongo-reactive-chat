package com.ctzn.springmongoreactivechat.mockclient;

import java.util.Collection;
import java.util.List;
import java.util.Map;
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
        sendMessage(messageFactory.getHello());
    }

    @Override
    public void sendMsg(String text) {
        sendMessage(messageFactory.getMsg(text));
    }

    @Override
    public void sendUpdateMe() {
        sendMessage(messageFactory.getUpdateMe());
    }

    @Override
    public void sendSetTyping() {
        sendMessage(messageFactory.getSetTyping());
    }

    @Override
    public void handleJson(String json) {
        messageHandler.handleJsonMessage(json);
    }

    @Override
    public Map<String, List<ServerMessage>> getMessageMap() {
        return messageHandler.getMessageMap();
    }

    @Override
    public Collection<ChatClient> getChatClients() {
        return messageHandler.getChatClients();
    }

    private void sendMessage(ClientMessage message) {
        jsonConsumer.accept(mapper.toJson(message));
    }
}
