package com.ctzn.springmongoreactivechat.service;

import com.ctzn.springmongoreactivechat.domain.dto.AttachmentModel;
import com.ctzn.springmongoreactivechat.service.videotranscoder.VideoTranscoderService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AttachmentHandlerService {

    private VideoTranscoderService videoTranscoderService;

    public AttachmentHandlerService(VideoTranscoderService videoTranscoderService) {
        this.videoTranscoderService = videoTranscoderService;
    }

    public void handle(List<AttachmentModel> attachments) {
        attachments.stream().filter(attachment -> attachment.getType() != null && attachment.getType().startsWith("video"))
                .forEach(attachment -> videoTranscoderService.addTask(attachment));
    }
}
