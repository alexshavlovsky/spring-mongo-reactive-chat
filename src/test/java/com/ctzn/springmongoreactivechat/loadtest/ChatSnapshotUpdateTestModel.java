package com.ctzn.springmongoreactivechat.loadtest;

import lombok.Data;

@Data
class ChatSnapshotUpdateTestModel {
    private Integer snapshotVer;
    private String type;
    private ChatClientTestModel user;
}
