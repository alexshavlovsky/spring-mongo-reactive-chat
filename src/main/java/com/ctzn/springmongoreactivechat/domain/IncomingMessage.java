package com.ctzn.springmongoreactivechat.domain;

import lombok.Data;

@Data
public class IncomingMessage {
    int frameId;
    String clientId;
    String userNick;
    String type;
    String payload;
}
