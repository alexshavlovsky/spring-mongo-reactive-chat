package com.ctzn.springmongoreactivechat.service;

import com.ctzn.springmongoreactivechat.domain.HttpEvent;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

class HttpUtil {
    static <T> Mono<T> newHttpError(Logger log, String remoteHost, HttpStatus status, String description) {
        log.error("[{}] {}", remoteHost, description);
        return Mono.error(new ResponseStatusException(status, description));
    }

    static <T> Mono<T> newHttpError(Logger log, ServerWebExchange exchange, HttpStatus status, String description) {
        return newHttpError(log, getRemoteHost(exchange), status, description);
    }

    static String getRemoteHost(ServerWebExchange exchange) {
        return HttpEvent.fromServerHttpRequest(exchange.getRequest()).getRemoteHost();
    }
}
