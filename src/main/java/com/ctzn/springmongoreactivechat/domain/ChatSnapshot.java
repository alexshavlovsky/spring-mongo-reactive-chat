package com.ctzn.springmongoreactivechat.domain;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Data
@RequiredArgsConstructor
public class ChatSnapshot {
    @NonNull
    private Integer snapshotVer;
    @NonNull
    private List<ChatClient> users;
    @NonNull
    private ChatClient thisUser;
}
