package com.ctzn.springmongoreactivechat.service;

import com.ctzn.springmongoreactivechat.domain.dto.AttachmentModel;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AttachmentHandlerService {
    public void handle(List<AttachmentModel> attachments) {
        System.out.println(attachments);
    }
}
