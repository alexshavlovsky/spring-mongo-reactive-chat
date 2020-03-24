package com.ctzn.springmongoreactivechat.domain;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@RequiredArgsConstructor
@Document
public class Person {
    @Id
    private String id;
    @NonNull
    private String firstName;
    @NonNull
    private String lastName;
}
