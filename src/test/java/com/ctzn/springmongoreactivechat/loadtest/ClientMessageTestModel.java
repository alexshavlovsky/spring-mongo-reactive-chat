package com.ctzn.springmongoreactivechat.loadtest;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
class ClientMessageTestModel {
    private int frameId;
    private String clientId;
    private String userNick;
    private String type;
    private String payload;
}
