package com.ctzn.springmongoreactivechat.concurrentloadtest.mockclient;

import lombok.Value;

import java.util.List;

@Value
class ChatSnapshot {
    int version;
    List<ChatClient> clients;
    ChatClient client;
}
