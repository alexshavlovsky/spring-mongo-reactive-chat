package com.ctzn.springmongoreactivechat.configuration;

import com.google.common.cache.CacheBuilder;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Arrays;

@Configuration
@EnableCaching
public class CachingConfig {

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        Cache thumbsCache = new ConcurrentMapCache("thumbs",
                CacheBuilder.newBuilder().expireAfterAccess(Duration.ofHours(1)).maximumSize(500).build().asMap(), false);
        cacheManager.setCaches(Arrays.asList(thumbsCache));
        return cacheManager;
    }
}
