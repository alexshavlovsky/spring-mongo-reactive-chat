package com.ctzn.springmongoreactivechat.service;

import com.ctzn.springmongoreactivechat.domain.HttpEvent;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public class HttpUtil {
    public static <T> Mono<T> newHttpError(Logger log, ServerWebExchange exchange, HttpStatus status, String prefix, String description) {
        return Mono.defer(() -> Mono.error(logAndGetError(log, getRemoteHost(exchange), status, prefix, description)));
    }

    private static Throwable logAndGetError(Logger log, String remoteHost, HttpStatus status, String prefix, String description) {
        log.error("{}->[{}] {}", prefix, remoteHost, description);
        return new ResponseStatusException(status, description);
    }

    public static String getRemoteHost(ServerWebExchange exchange) {
        return HttpEvent.fromServerHttpRequest(exchange.getRequest()).getRemoteHost();
    }
}
