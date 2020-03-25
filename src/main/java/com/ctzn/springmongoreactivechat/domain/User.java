package com.ctzn.springmongoreactivechat.domain;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@RequiredArgsConstructor
@Document
@TypeAlias("user")
public class User {
    @Id
    private String id;
    @NonNull
    private String nickName;

    private Date createdTimestamp = new Date();

    private Date seenTimestamp = new Date();

    @Version
    private Long version;
}
