package com.ctzn.springmongoreactivechat.loadtest;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
class ClientMessageTestModel {
    int frameId;
    String clientId;
    String userNick;
    String type;
    String payload;
}
