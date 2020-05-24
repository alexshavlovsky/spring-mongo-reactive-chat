package com.ctzn.springmongoreactivechat.reactiveloadtest;

import com.google.gson.Gson;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

abstract class AbstractTestClient {
    private final ChatClient chatClient = new ChatClient();
    final ClientMessageFactory clientMessageFactory = new ClientMessageFactory(chatClient);

    AbstractTestClient() {
        this(UUID.randomUUID().toString());
    }

    AbstractTestClient(String nick) {
        chatClient.setClientId(UUID.randomUUID().toString());
        chatClient.setNick(nick);
    }

    private int snapshotVersion = -1;
    private final List<ServerMessage> messages = new ArrayList<>();
    private final Map<String, ChatClient> chatSnapshot = new HashMap<>();
    final Gson gson = new Gson();

    void handleServerMessage(String rawText) {
        if ("PUBLISH".equals(rawText)) {
            System.out.println(Thread.currentThread().getName());
            publishResults();
            return;
        }
        ServerMessage msg = gson.fromJson(rawText, ServerMessage.class);
        messages.add(msg);
        if ("snapshot".equals(msg.getType())) {
            ChatSnapshot snapshot = gson.fromJson(msg.getPayload(), ChatSnapshot.class);
            snapshotVersion = snapshot.getSnapshotVer();
            snapshot.getUsers().forEach(u -> chatSnapshot.put(u.getSessionId(), u));
        }
        if ("snapshotUpdate".equals(msg.getType())) {
            ChatSnapshotUpdate update = gson.fromJson(msg.getPayload(), ChatSnapshotUpdate.class);
            if (update.getSnapshotVer() >= snapshotVersion) {
                ChatClient user = update.getUser();
                switch (update.getType()) {
                    case "updateUser":
                    case "addUser":
                        chatSnapshot.put(user.getSessionId(), user);
                        break;
                    case "removeUser":
                        chatSnapshot.remove(user.getSessionId());
                        break;
                }
            }
        }
    }

    String getNick() {
        return chatClient.getNick();
    }

    String getClientId() {
        return chatClient.getClientId();
    }

    static class FinalHolder {
        final Collection<ChatClient> snapshot;
        final Map<String, List<ServerMessage>> messagesMap;

        FinalHolder(Collection<ChatClient> snapshot, Map<String, List<ServerMessage>> messagesMap) {
            this.snapshot = snapshot;
            this.messagesMap = messagesMap;
        }
    }

    private FinalHolder resultHolder;

    void publishResults() {
        if (resultHolder != null) throw new RuntimeException("Only one publication per instance is allowed");
        resultHolder = new FinalHolder(
                chatSnapshot.values(),
                messages.stream().collect(HashMap::new, (m, v) -> m.merge(v.getType(), Stream.of(v).collect(Collectors.toList()), (a, n) -> {
                    a.addAll(n);
                    return a;
                }), Map::putAll)
        );
    }

    private void sleep() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
        }
    }

    Collection<ChatClient> getSnapshot() {
        for (int i = 0; i < 10 && resultHolder == null; i++) sleep();
        return resultHolder == null ? null : resultHolder.snapshot;
    }

    Map<String, List<ServerMessage>> getMessagesMap() {
        for (int i = 0; i < 10 && resultHolder == null; i++) sleep();
        return resultHolder == null ? null : resultHolder.messagesMap;
    }
}
