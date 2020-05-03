package com.ctzn.springmongoreactivechat.service;

import com.ctzn.springmongoreactivechat.domain.HttpEvent;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Function;

class HttpUtil {
    static private String FORM_DATA_FILE_ID_KEY = "fileId";

    static Mono<List<String>> savePartsAndCollectIds(Logger log, Flux<FilePart> parts, ServerWebExchange exchange, Function<Flux<FilePart>, Publisher<String>> saveFiles) {
        return parts
                .transform(saveFiles)
                .doOnNext(fileId -> log.info("<-f[{}] {}", getRemoteHost(exchange), fileId))
                .collectList();
    }

    static Mono<?> formDataParseFileId(ServerWebExchange exchange, Logger log) {
        return exchange.getFormData().flatMap(formData -> {
            String fileId = formData.getFirst(FORM_DATA_FILE_ID_KEY);
            if (fileId == null) return newHttpError(log, exchange, HttpStatus.BAD_REQUEST,
                    "Form field is required: " + FORM_DATA_FILE_ID_KEY);
            return Mono.just(fileId);
        });
    }

    static Mono<Void> newHttpError(Logger log, String remoteHost, HttpStatus status, String description) {
        log.error("[{}] {}", remoteHost, description);
        return Mono.error(new ResponseStatusException(status, description));
    }

    static Mono<Void> newHttpError(Logger log, ServerWebExchange exchange, HttpStatus status, String description) {
        return newHttpError(log, getRemoteHost(exchange), status, description);
    }

    static String getRemoteHost(ServerWebExchange exchange) {
        return HttpEvent.fromServerHttpRequest(exchange.getRequest()).getRemoteHost();
    }
}
