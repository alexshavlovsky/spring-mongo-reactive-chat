package com.ctzn.springmongoreactivechat.mockclient;

import lombok.Value;

@Value
class ChatSnapshotUpdate {
    int version;
    String type;
    ChatClient client;
}
