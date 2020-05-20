package com.ctzn.springmongoreactivechat.loadtest;

import lombok.Data;

@Data
class ServerMessageTestModel {
    private String id;
    private String sessionId;
    private String clientId;
    private String userNick;
    private String type;
    private String payload;
    private Long timestamp;
}
