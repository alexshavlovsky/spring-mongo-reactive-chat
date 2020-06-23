package com.ctzn.springmongoreactivechat.service.thumbs;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

@Service
@Qualifier("caffeine-caching-proxy")
public class ThumbsServiceCaffeineCachingProxy implements ThumbsService {

    private ThumbsService thumbsService;

    public ThumbsServiceCaffeineCachingProxy(@Qualifier("subject") ThumbsService thumbsService) {
        this.thumbsService = thumbsService;
    }

    @Override
    public MediaType getMediaType() {
        return thumbsService.getMediaType();
    }

    private Cache<ThumbKey, Mono<byte[]>> cache = Caffeine
            .newBuilder()
            .maximumSize(1_000)
            .expireAfterAccess(60, TimeUnit.MINUTES)
            .build();

    @Override
    public Mono<byte[]> getThumb(ThumbKey key) {
        // see https://github.com/reactor/reactor-addons/issues/162#issuecomment-414256993
        return cache.asMap().computeIfAbsent(key, k -> thumbsService.getThumb(k).cache());
    }
}
