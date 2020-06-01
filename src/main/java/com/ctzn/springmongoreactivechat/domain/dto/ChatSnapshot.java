package com.ctzn.springmongoreactivechat.domain.dto;

import lombok.Value;

import java.util.List;

@Value
public class ChatSnapshot {
    int version;
    List<ChatClient> clients;
    ChatClient client;
}
