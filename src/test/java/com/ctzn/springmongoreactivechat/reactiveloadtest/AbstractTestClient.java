package com.ctzn.springmongoreactivechat.reactiveloadtest;

import com.google.gson.Gson;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

abstract class AbstractTestClient {
    private final ChatClient chatClient;
    final ClientMessageFactory clientMessageFactory;

    AbstractTestClient() {
        chatClient = new ChatClient(null, UUID.randomUUID().toString(), UUID.randomUUID().toString());
        clientMessageFactory = new ClientMessageFactory(chatClient);
    }

    private int snapshotVersion = -1;
    private final List<ServerMessage> messages = new ArrayList<>();
    private final Map<String, ChatClient> chatSnapshot = new HashMap<>();
    final Gson gson = new Gson();

    abstract boolean publishStrategy(ServerMessage msg);

    void handleServerMessage(ServerMessage msg) {
        if (resultHolder != null) return;
        if (publishStrategy(msg)) {
            publishResults();
            return;
        }
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

    private void publishResults() {
        if (resultHolder != null) throw new RuntimeException("Only one publication per instance is allowed");
        resultHolder = new FinalHolder(
                chatSnapshot.values(),
                messages.stream().collect(HashMap::new, (m, v) -> m.merge(v.getType(), Stream.of(v).collect(Collectors.toList()), (a, n) -> {
                    a.addAll(n);
                    return a;
                }), Map::putAll)
        );
    }

    Collection<ChatClient> getSnapshot() {
        return resultHolder == null ? null : resultHolder.snapshot;
    }

    Map<String, List<ServerMessage>> getMessagesMap() {
        return resultHolder == null ? null : resultHolder.messagesMap;
    }
}
