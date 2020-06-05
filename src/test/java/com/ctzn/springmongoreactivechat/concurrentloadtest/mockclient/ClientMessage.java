package com.ctzn.springmongoreactivechat.concurrentloadtest.mockclient;

import lombok.Value;

@Value
class ClientMessage {
    int frameId;
    User user;
    String type;
    String payload;
}
