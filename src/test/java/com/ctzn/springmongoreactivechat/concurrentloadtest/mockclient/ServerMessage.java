package com.ctzn.springmongoreactivechat.concurrentloadtest.mockclient;

import lombok.Value;

import java.util.Date;

@Value
public class ServerMessage {
    ChatClient client;
    String type;
    String payload;
    Date timestamp = new Date();
}
