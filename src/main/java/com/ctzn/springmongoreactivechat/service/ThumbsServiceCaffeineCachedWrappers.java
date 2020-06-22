package com.ctzn.springmongoreactivechat.service;

import com.github.alexpumpkin.reactorlock.concurrency.LockCacheMono;
import com.github.alexpumpkin.reactorlock.concurrency.LockMono;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;

import java.util.concurrent.TimeUnit;

@Service
@Qualifier("proxy-caffeine-wrappers")
public class ThumbsServiceCaffeineCachedWrappers implements ThumbsService {

    private ThumbsService thumbsService;

    public ThumbsServiceCaffeineCachedWrappers(@Qualifier("subject") ThumbsService thumbsService) {
        this.thumbsService = thumbsService;
    }

    @Override
    public MediaType getMediaType() {
        return thumbsService.getMediaType();
    }

    private Cache<ThumbKey, Signal<byte[]>> cache = Caffeine
            .newBuilder()
            .maximumSize(1_000)
            .expireAfterAccess(60, TimeUnit.MINUTES)
            .build();

    @Override
    public Mono<byte[]> getThumb(ThumbKey key) {
        return LockCacheMono
                .create(LockMono.key(key).build())
                .lookup(cache.asMap())
                .onCacheMissResume(() -> Mono.defer(() -> thumbsService.getThumb(key)));
        // see also https://en.wikipedia.org/wiki/Cache_stampede
        // see also https://stackoverflow.com/questions/58182516/cacheflux-how-to-synchronize-lookup-oncachemissresume
        // see also https://github.com/reactor/reactor-addons/issues/234
        // see also https://github.com/alex-pumpkin/reactor-lock
    }
}
