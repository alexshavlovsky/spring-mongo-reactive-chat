package com.ctzn.springmongoreactivechat.domain;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class ChatClient {
    @NonNull
    private String sessionId;
    private String clientId;
    private String nick;

    public static ChatClient newInstance(String sessionId) {
        return new ChatClient(sessionId);
    }
}
