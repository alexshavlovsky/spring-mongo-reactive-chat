package com.ctzn.springmongoreactivechat.service.ffmpeglocator;

import org.springframework.stereotype.Service;
import ws.schild.jave.process.ProcessLocator;

@Service
public class FfmpegLocatorService {
    private final ProcessLocator instance = new CustomFfmpegLocator();

    public ProcessLocator getInstance() {
        return instance;
    }
}
