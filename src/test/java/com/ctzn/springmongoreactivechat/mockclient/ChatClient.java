package com.ctzn.springmongoreactivechat.mockclient;

import lombok.Value;

@Value
public class ChatClient {
    String sessionId;
    String clientId;
    String nick;
}
