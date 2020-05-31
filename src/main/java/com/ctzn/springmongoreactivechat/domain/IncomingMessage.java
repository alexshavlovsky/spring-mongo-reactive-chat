package com.ctzn.springmongoreactivechat.domain;

import lombok.Data;

@Data
public class IncomingMessage {
    private final int frameId;
    private final String clientId;
    private final String nick;
    private final String type;
    private final String payload;
}
