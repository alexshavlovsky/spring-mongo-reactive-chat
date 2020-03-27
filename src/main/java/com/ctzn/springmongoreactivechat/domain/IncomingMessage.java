package com.ctzn.springmongoreactivechat.domain;

import lombok.Data;

@Data
public class IncomingMessage {
    String clientId;
    String clientName;
    String messageText;
}
