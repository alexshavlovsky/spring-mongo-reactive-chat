package com.ctzn.springmongoreactivechat.domain.dto;

import lombok.Value;

@Value
public class ChatSnapshotUpdate {
    int version;
    String type;
    ChatClient client;
}
