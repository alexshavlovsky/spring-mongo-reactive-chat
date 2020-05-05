package com.ctzn.springmongoreactivechat.service;

import com.ctzn.springmongoreactivechat.domain.HttpEvent;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

class HttpUtil {
    static <T> Mono<T> newHttpError(Logger log, ServerWebExchange exchange, HttpStatus status, String description) {
        return Mono.defer(() -> Mono.error(logAndGetError(log, getRemoteHost(exchange), status, description)));
    }

    private static Throwable logAndGetError(Logger log, String remoteHost, HttpStatus status, String description) {
        log.error("f->[{}] {}", remoteHost, description);
        return new ResponseStatusException(status, description);
    }

    static String getRemoteHost(ServerWebExchange exchange) {
        return HttpEvent.fromServerHttpRequest(exchange.getRequest()).getRemoteHost();
    }
}
