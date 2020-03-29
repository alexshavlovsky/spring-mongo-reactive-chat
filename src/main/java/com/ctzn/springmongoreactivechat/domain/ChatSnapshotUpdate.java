package com.ctzn.springmongoreactivechat.domain;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class ChatSnapshotUpdate {
    @NonNull
    private Integer snapshotVer;
    @NonNull
    private String type;
    @NonNull
    private ChatClient user;
}
