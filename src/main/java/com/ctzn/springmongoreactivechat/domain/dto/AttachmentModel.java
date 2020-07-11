package com.ctzn.springmongoreactivechat.domain.dto;

import lombok.Value;

import java.util.Date;

@Value
public class AttachmentModel {
    String fileId;
    String name;
    long size;
    Date lastModified;
    String type;
}
