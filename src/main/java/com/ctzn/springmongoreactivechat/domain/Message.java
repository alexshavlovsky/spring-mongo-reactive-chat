package com.ctzn.springmongoreactivechat.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.web.reactive.socket.WebSocketSession;

import java.util.Date;

@Data
@RequiredArgsConstructor
@Document
@TypeAlias("messages")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Message {
    @Id
    private String id;
    @NonNull
    private String sessionId;
    @NonNull
    private String clientId;
    @NonNull
    private String userNick;
    @NonNull
    private String type;
    @NonNull
    private String payload;
    private Date timestamp = new Date();

    public static Message newInfo(String message) {
        return new Message("", "", "", "info", message);
    }

    public static Message newText(WebSocketSession session, IncomingMessage message) {
        return new Message(session.getId(), message.getClientId(), message.getUserNick() == null ? "" : message.getUserNick(), message.type, message.getPayload());
    }

    public static Message newObject(String type, String payload) {
        return new Message("", "", "", type, payload);
    }
}
