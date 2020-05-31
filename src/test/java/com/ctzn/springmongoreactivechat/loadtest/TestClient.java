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

    private final String clientId = UUID.randomUUID().toString();
    private final String nick;
    private final boolean putOnlySnapshot;
    private final Gson gson = new Gson();

    private final List<ServerMessageTestModel> messages = new ArrayList<>();
    private final Map<String, ChatClientTestModel> chatSnapshot = new HashMap<>();

    private int frameId = 0;
    private int snapshotVersion = -1;

    final CountDownLatch snapshotLatch = new CountDownLatch(1);
    final CountDownLatch disconnectLatch = new CountDownLatch(1);

    private TestClient(URI serverUri, String nickPrefix, boolean putOnlySnapshot) {
        super(serverUri);
        nick = nickPrefix + ("" + (1000 + ++instanceCounter)).substring(1);
        this.putOnlySnapshot = putOnlySnapshot;
    }

    static TestClient newInstance(String serverUri, String nickPrefix, boolean putOnlySnapshot) {
        return new TestClient(URI.create(serverUri), nickPrefix, putOnlySnapshot);
    }

    private void sendTypedMessage(String type, String payload) {
        ClientMessageTestModel msg = new ClientMessageTestModel(frameId++, clientId, nick, type, payload);
        send(gson.toJson(msg));
    }

    private void sendHello() {
        sendTypedMessage("hello", "");
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
        if (disconnectLatch.getCount() == 0) return;
        ServerMessageTestModel msg = gson.fromJson(message, ServerMessageTestModel.class);
        if ("snapshot".equals(msg.getType())) {
            ChatSnapshotTestModel snapshot = gson.fromJson(msg.getPayload(), ChatSnapshotTestModel.class);
            snapshotVersion = snapshot.getSnapshotVer();
            snapshot.getUsers().forEach(u -> chatSnapshot.put(u.getSessionId(), u));
            snapshotLatch.countDown();
        }
        if ("snapshotUpdate".equals(msg.getType())) {
            ChatSnapshotUpdateTestModel update = gson.fromJson(msg.getPayload(), ChatSnapshotUpdateTestModel.class);
            if (update.getSnapshotVer() >= snapshotVersion) {
                ChatClientTestModel user = update.getUser();
                switch (update.getType()) {
                    case "updateUser":
                    case "addUser":
                        chatSnapshot.put(user.getSessionId(), user);
                        break;
                    case "removeUser":
                        if (!putOnlySnapshot) chatSnapshot.remove(user.getSessionId());
                        break;
                }
            }
        }
        messages.add(msg);
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        sendHello();
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        if (remote)
            System.out.println("Client has been disconnected from server with code: " + code + " " + reason);
        disconnectLatch.countDown();
    }

    @Override
    public void onError(Exception ex) {
        System.out.println("Exception occurred ...\n" + ex);
        disconnectLatch.countDown();
    }

    String getNick() {
        return nick;
    }

    // TODO rewrite all the thread-safety logic
    synchronized Set<ChatClientTestModel> getChatSnapshot() {
        return new HashSet<>(chatSnapshot.values());
    }

    synchronized Map<String, List<ServerMessageTestModel>> getMessageMap() {
        return messages.stream()
                .collect(HashMap::new, (m, v) -> m.merge(v.getType(), Stream.of(v).collect(Collectors.toList()), (a, n) -> {
                    a.addAll(n);
                    return a;
                }), Map::putAll);
    }
}
