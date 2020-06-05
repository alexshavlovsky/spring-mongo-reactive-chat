package com.ctzn.springmongoreactivechat.concurrentloadtest.mockclient;

import lombok.Value;

@Value
class ChatSnapshotUpdate {
    int version;
    String type;
    ChatClient client;
}
