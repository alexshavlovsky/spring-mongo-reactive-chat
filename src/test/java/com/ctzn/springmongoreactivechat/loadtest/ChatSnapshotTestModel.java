package com.ctzn.springmongoreactivechat.loadtest;

import lombok.Data;

import java.util.List;

@Data
class ChatSnapshotTestModel {
    private Integer snapshotVer;
    private List<ChatClientTestModel> users;
    private ChatClientTestModel thisUser;
}
