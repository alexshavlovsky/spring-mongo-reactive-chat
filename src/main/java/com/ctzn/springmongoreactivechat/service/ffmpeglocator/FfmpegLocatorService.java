package com.ctzn.springmongoreactivechat.service.ffmpeglocator;

import org.springframework.stereotype.Service;
import ws.schild.jave.FFMPEGLocator;

@Service
public class FfmpegLocatorService {
    private final FFMPEGLocator instance = new CustomFfmpegLocator();

    public FFMPEGLocator getInstance() {
        return instance;
    }
}
