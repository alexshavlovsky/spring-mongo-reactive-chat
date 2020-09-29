package com.ctzn.springmongoreactivechat.service.thumbs;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.core.io.buffer.DataBuffer;

public interface ThumbBuilderResolver {
    Thumbnails.Builder resolve(String thumbType, DataBuffer dataBuffer) throws Exception;
}
