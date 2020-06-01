package com.ctzn.springmongoreactivechat.domain;

import com.ctzn.springmongoreactivechat.domain.dto.ChatClient;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Document
@TypeAlias("messages")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Message {
    @Id
    private String id;
    final private ChatClient client;
    final private String type;
    final private String payload;
    final private Date timestamp = new Date();

    private static Message newServerMessage(String type, String payload) {
        return new Message(null, type, payload);
    }

    public static Message newInfo(String text) {
        return newServerMessage("info", text);
    }

    static Message newSnapshot(String payload) {
        return newServerMessage("snapshot", payload);
    }

    static Message newSnapshotUpdate(String payload) {
        return newServerMessage("snapshotUpdate", payload);
    }
}
