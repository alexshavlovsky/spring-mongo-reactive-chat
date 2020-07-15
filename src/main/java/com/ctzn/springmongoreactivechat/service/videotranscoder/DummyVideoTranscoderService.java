package com.ctzn.springmongoreactivechat.service.videotranscoder;

import com.ctzn.springmongoreactivechat.domain.dto.AttachmentModel;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!mongo-video-transcoder")
public class DummyVideoTranscoderService implements VideoTranscoderService {
    @Override
    public void addTask(AttachmentModel attachment) {
        // this method does nothing
    }
}
