package com.ctzn.springmongoreactivechat.service;

import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

public interface ThumbsService {
    MediaType getMediaType();

    Mono<byte[]> getThumb(String fileId, String thumbType);
}
