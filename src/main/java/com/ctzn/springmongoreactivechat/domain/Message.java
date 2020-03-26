package com.ctzn.springmongoreactivechat.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@RequiredArgsConstructor
@Document
@TypeAlias("messages")
public class Message {
    @Id
    private String id;
    @NonNull
    private String type;
    @NonNull
    private String author;
    @NonNull
    private String text;
    private Date timestamp = new Date();

    private static final ObjectMapper json = new ObjectMapper();

    public String asJson() {
        try {
            return json.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

}
