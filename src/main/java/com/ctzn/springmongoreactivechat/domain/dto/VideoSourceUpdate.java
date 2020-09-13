package com.ctzn.springmongoreactivechat.domain.dto;

import com.ctzn.springmongoreactivechat.domain.CompoundWebVideo;
import lombok.Value;

@Value
public class VideoSourceUpdate {
    String attachmentId;
    CompoundWebVideo compoundWebVideo;
}
