package com.ctzn.springmongoreactivechat.domain;

import lombok.Data;

import java.util.List;

@Data
public class ChatSnapshot {
    private final Integer snapshotVer;
    private final List<ChatClient> users;
    private final ChatClient thisUser;
}
