package com.ctzn.springmongoreactivechat.concurrentloadtest.mockclient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Value;

import java.util.Date;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServerMessage {
    ChatClient client;
    String type;
    String payload;
    Date timestamp = new Date();
}
