package com.ctzn.springmongoreactivechat.domain.dto;

import com.ctzn.springmongoreactivechat.domain.Message;
import lombok.Value;

@Value
public class IncomingMessage {
    int frameId;
    User user;
    String type;
    String payload;

    public Message toMessage(String sessionId) {
        return new Message(user.toChatClient(sessionId), type, payload);
    }
}
