package com.ctzn.springmongoreactivechat.mockclient;

import lombok.Value;

import java.util.List;

@Value
class ChatSnapshot {
    int version;
    List<ChatClient> clients;
    ChatClient client;
}
