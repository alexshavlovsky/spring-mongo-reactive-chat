package com.ctzn.springmongoreactivechat.mockclient;

import java.util.*;

class ChatSnapshotHandlerImpl implements ChatSnapshotHandler {
    private final Map<String, ChatClient> chatSnapshot = Collections.synchronizedMap(new HashMap<>());
    private int version = -1;

    @Override
    public Collection<ChatClient> getClients() {
        return new ArrayList<>(chatSnapshot.values());
    }

    @Override
    public void handleSnapshot(ChatSnapshot snapshot) {
        version = snapshot.getVersion();
        snapshot.getClients().forEach(client -> chatSnapshot.put(client.getSessionId(), client));
    }

    @Override
    public void handleSnapshotUpdate(ChatSnapshotUpdate update) {
        if (update.getVersion() >= version) {
            ChatClient client = update.getClient();
            switch (update.getType()) {
                case "updateUser":
                case "addUser":
                    chatSnapshot.put(client.getSessionId(), client);
                    break;
                case "removeUser":
                    chatSnapshot.remove(client.getSessionId());
                    break;
            }
        }
    }
}
