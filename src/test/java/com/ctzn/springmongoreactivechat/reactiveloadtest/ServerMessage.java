package com.ctzn.springmongoreactivechat.reactiveloadtest;

import lombok.Data;

@Data
class ServerMessage {
    private String id;
    private String sessionId;
    private String clientId;
    private String nick;
    private String type;
    private String payload;
    private Long timestamp;
}
