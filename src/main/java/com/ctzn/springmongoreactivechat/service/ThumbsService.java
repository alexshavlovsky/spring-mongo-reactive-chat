package com.ctzn.springmongoreactivechat.service;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

public interface ThumbsService {
    byte[] getThumb(String fileId, DataBuffer dataBuffer) throws Exception;

    Mono<byte[]> getCachedOrGenerate(String fileId, Mono<byte[]> generator);

    MediaType getMediaType();
}
