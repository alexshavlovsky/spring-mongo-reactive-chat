package com.ctzn.springmongoreactivechat.reactiveloadtest;

import lombok.Data;

@Data
class ChatClient {
    private final String sessionId;
    private final String clientId;
    private final String nick;
}
