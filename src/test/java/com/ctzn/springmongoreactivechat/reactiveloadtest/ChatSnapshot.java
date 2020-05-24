package com.ctzn.springmongoreactivechat.reactiveloadtest;

import lombok.Data;

import java.util.List;

@Data
class ChatSnapshot {
    private Integer snapshotVer;
    private List<ChatClient> users;
    private ChatClient thisUser;
}
