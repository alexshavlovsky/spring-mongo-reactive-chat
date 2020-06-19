package com.ctzn.springmongoreactivechat.service;

import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

import java.util.Objects;

public interface ThumbsService {

    class ThumbKey {
        String fileId;
        String thumbType;

        public ThumbKey(String fileId, String thumbType) {
            this.fileId = fileId;
            this.thumbType = thumbType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ThumbKey thumbKey = (ThumbKey) o;
            return Objects.equals(fileId, thumbKey.fileId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(fileId);
        }
    }

    MediaType getMediaType();

    Mono<byte[]> getThumb(ThumbKey key);
}
