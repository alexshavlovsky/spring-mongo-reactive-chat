package com.ctzn.springmongoreactivechat.service;

import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;

@Service
public class ThumbsServiceJpg240x240 implements ThumbsService {
    private Logger LOG = LoggerFactory.getLogger(ThumbsServiceJpg240x240.class);
    private CacheManager cacheManager;

    public ThumbsServiceJpg240x240(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    @Cacheable(value = "thumbs", key = "#fileId")
    public byte[] getThumb(String fileId, DataBuffer dataBuffer) throws Exception {
        LOG.info("Generate thumbnail of {}", fileId);
        ByteArrayOutputStream arrayBuffer = new ByteArrayOutputStream();
        Thumbnails.of(dataBuffer.asInputStream(true))
                .size(240, 240).outputFormat("jpg").toOutputStream(arrayBuffer);
        return arrayBuffer.toByteArray();
    }

    @Override
    public Mono<byte[]> getCachedOrGenerate(String fileId, Mono<byte[]> thumbPublisher) {
        Cache cache = cacheManager.getCache("thumbs");
        if (cache == null) return thumbPublisher;
        byte[] cachedThumb = cache.get(fileId, byte[].class);
        if (cachedThumb == null) return thumbPublisher;
        return Mono.just(cachedThumb);
    }

    @Override
    public MediaType getMediaType() {
        return MediaType.IMAGE_JPEG;
    }
}
