package com.ctzn.springmongoreactivechat.domain.dto;

import lombok.Value;

@Value
public class User {
    String id;
    String nick;

    public ChatClient toChatClient(String sessionId) {
        return new ChatClient(sessionId, id, nick);
    }
}
