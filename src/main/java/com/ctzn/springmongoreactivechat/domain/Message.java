package com.ctzn.springmongoreactivechat.domain;

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
@TypeAlias("message")
public class Message {
    @Id
    private String id;
    @NonNull
    private String text;
    private Date timestamp = new Date();
}
