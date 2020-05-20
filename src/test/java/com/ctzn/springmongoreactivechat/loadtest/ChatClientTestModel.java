package com.ctzn.springmongoreactivechat.loadtest;

import lombok.Data;

@Data
class ChatClientTestModel {
    String sessionId;
    String clientId;
    String nick;
}
