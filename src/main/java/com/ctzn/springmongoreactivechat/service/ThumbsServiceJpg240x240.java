package com.ctzn.springmongoreactivechat.service;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.cache.CacheManager;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
public class ThumbsServiceJpg240x240 extends CachedThumbService {

    public ThumbsServiceJpg240x240(CacheManager cacheManager) {
        super(cacheManager);
    }

    @Override
    byte[] generateThumb(DataBuffer dataBuffer) throws Exception {
        ByteArrayOutputStream arrayBuffer = new ByteArrayOutputStream();
        Thumbnails.of(dataBuffer.asInputStream(true))
                .size(240, 240).outputFormat("jpg").toOutputStream(arrayBuffer);
        return arrayBuffer.toByteArray();
    }

    @Override
    public MediaType getMediaType() {
        return MediaType.IMAGE_JPEG;
    }
}
