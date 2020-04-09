package com.ctzn.springmongoreactivechat.domain;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class ChatClient {
    @NonNull
    String sessionId;
    String clientId;
    String nick;

    public static ChatClient newInstance(String sessionId) {
        return new ChatClient(sessionId);
    }
}
