package com.ctzn.springmongoreactivechat.reactiveloadtest;

import lombok.Data;

@Data
class ChatSnapshotUpdate {
    private Integer snapshotVer;
    private String type;
    private ChatClient user;
}
