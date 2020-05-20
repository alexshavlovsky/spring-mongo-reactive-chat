package com.ctzn.springmongoreactivechat.loadtest;

import com.google.gson.Gson;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestClient extends WebSocketClient {
    static private int instanceCounter = 0;

    private final List<ServerMessageTestModel> messages = new ArrayList<>();
    private final String clientId = UUID.randomUUID().toString();
    private final String nick;
    private final Gson gson = new Gson();
    private int frameId = 0;

    final CountDownLatch snapshotLatch = new CountDownLatch(1);
    final CountDownLatch snapshotUpdateSelfLatch = new CountDownLatch(1);
    final CountDownLatch disconnectLatch = new CountDownLatch(1);

    Map<String, List<ServerMessageTestModel>> messagesMap = null;

    private TestClient(URI serverUri, String nickPrefix) {
        super(serverUri);
        nick = nickPrefix + ("" + (1000 + ++instanceCounter)).substring(1);
    }

    static TestClient newInstance(String serverUri, String nickPrefix) {
        return new TestClient(URI.create(serverUri), nickPrefix);
    }

    private void sendTypedMessage(String type, String payload) {
        ClientMessageTestModel msg = new ClientMessageTestModel(frameId++, clientId, nick, type, payload);
        send(gson.toJson(msg));
    }

    private void updateClientDetails() {
        sendTypedMessage("updateMe", "");
    }

    void setTyping() {
        sendTypedMessage("setTyping", "");
    }

    void sendMsg(String text) {
        sendTypedMessage("msg", text);
    }

    @Override
    public void onMessage(String message) {
        ServerMessageTestModel msg = gson.fromJson(message, ServerMessageTestModel.class);
        if ("snapshot".equals(msg.getType())) snapshotLatch.countDown();
        if ("snapshotUpdate".equals(msg.getType())) {
            ChatSnapshotUpdateTestModel update = gson.fromJson(msg.getPayload(), ChatSnapshotUpdateTestModel.class);
            if ("updateUser".equals(update.getType()) && nick.equals(update.getUser().getNick()))
                snapshotUpdateSelfLatch.countDown();
        }
        messages.add(msg);
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        updateClientDetails();
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        publishMessagesMap();
        if (remote)
            System.out.println("Client has been disconnected from server with code: " + code + " " + reason);
    }

    @Override
    public void onError(Exception ex) {
        publishMessagesMap();
        System.out.println("Exception occurred ...\n" + ex);
    }

    private void publishMessagesMap() {
        messagesMap = messages.stream()
                .collect(HashMap::new, (m, v) -> m.merge(v.getType(), Stream.of(v).collect(Collectors.toList()), (a, n) -> {
                    a.addAll(n);
                    return a;
                }), Map::putAll);
        disconnectLatch.countDown();
    }

    String getNick() {
        return nick;
    }
}
