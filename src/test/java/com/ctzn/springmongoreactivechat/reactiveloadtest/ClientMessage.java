package com.ctzn.springmongoreactivechat.reactiveloadtest;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
class ClientMessage {
    private int frameId;
    private String clientId;
    private String nick;
    private String type;
    private String payload;
}
