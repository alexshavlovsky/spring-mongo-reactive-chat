package com.ctzn.springmongoreactivechat.domain.dto;

import com.ctzn.springmongoreactivechat.domain.Message;
import lombok.Value;

import static com.ctzn.springmongoreactivechat.domain.Message.newMessage;

@Value
public class IncomingMessage {
    int frameId;
    User user;
    String type;
    String payload;

    public Message toMessage(String sessionId) {
        return newMessage(user.toChatClient(sessionId), type, payload);
    }
}
