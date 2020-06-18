package com.ctzn.springmongoreactivechat.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Mono;

import java.io.IOException;

public abstract class CachedThumbService implements ThumbsService {

    private Logger LOG = LoggerFactory.getLogger(CachedThumbService.class);
    private CacheManager cacheManager;

    public CachedThumbService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    @Cacheable(value = "thumbs", key = "#fileId")
    public byte[] getThumb(String fileId, String thumbType, DataBuffer dataBuffer) throws IOException {
        LOG.info("Generate {} thumbnail of {}", thumbType, fileId);
        return generateThumb(thumbType, dataBuffer);
    }

    abstract byte[] generateThumb(String thumbType, DataBuffer dataBuffer) throws IOException;

    @Override
    public Mono<byte[]> getCachedOrGenerate(String fileId, Mono<byte[]> thumbPublisher) {
        Cache cache = cacheManager.getCache("thumbs");
        if (cache == null) return thumbPublisher;
        byte[] cachedThumb = cache.get(fileId, byte[].class);
        if (cachedThumb == null) return thumbPublisher;
        return Mono.just(cachedThumb);
    }
}
