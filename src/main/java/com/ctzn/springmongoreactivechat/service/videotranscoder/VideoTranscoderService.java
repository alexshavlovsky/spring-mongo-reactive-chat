package com.ctzn.springmongoreactivechat.service.videotranscoder;

import com.ctzn.springmongoreactivechat.domain.dto.AttachmentModel;

public interface VideoTranscoderService {
    void addTask(AttachmentModel attachment);
}
