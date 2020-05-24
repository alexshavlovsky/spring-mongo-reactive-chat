package com.ctzn.springmongoreactivechat.reactiveloadtest;

import lombok.Data;

@Data
class ChatClient {
    private String sessionId;
    private String clientId;
    private String nick;
}
