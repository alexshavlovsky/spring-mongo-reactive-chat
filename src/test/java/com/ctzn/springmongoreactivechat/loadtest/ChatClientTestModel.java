package com.ctzn.springmongoreactivechat.loadtest;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
class ChatClientTestModel {
    @EqualsAndHashCode.Include
    private String sessionId;
    private String clientId;
    private String nick;
}
