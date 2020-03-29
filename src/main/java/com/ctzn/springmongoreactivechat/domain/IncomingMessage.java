package com.ctzn.springmongoreactivechat.domain;

import lombok.Data;

@Data
public class IncomingMessage {
    String remoteClientId;
    String messageText;
}
