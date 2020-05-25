package com.ctzn.springmongoreactivechat.domain;

import lombok.Data;

@Data
public class IncomingMessage {
    private int frameId;
    private String clientId;
    private String userNick;
    private String type;
    private String payload;
}
