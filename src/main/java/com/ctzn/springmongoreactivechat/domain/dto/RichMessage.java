package com.ctzn.springmongoreactivechat.domain.dto;

import lombok.Value;

import java.util.List;

@Value
public class RichMessage {
    String message;
    List<AttachmentModel> attachments;
}
