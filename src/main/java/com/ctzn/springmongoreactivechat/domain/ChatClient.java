package com.ctzn.springmongoreactivechat.domain;

import lombok.Data;
import lombok.ToString;

@Data
public class ChatClient {
    @ToString.Exclude
    private final String sessionId;
    private final String clientId;
    private final String nick;

    public static ChatClient fromMessage(String sessionId, IncomingMessage message) {
        return new ChatClient(sessionId, message.getClientId(), message.getNick());
    }
}
