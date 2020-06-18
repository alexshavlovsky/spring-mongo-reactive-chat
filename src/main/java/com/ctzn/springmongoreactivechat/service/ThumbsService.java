package com.ctzn.springmongoreactivechat.service;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

import java.io.IOException;

public interface ThumbsService {
    byte[] getThumb(String fileId, String thumbType, DataBuffer dataBuffer) throws IOException;

    Mono<byte[]> getCachedOrGenerate(String fileId, Mono<byte[]> generator);

    MediaType getMediaType();
}
