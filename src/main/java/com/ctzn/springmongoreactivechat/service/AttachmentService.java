package com.ctzn.springmongoreactivechat.service;

import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface AttachmentService {
    Mono<List<String>> saveAttachments(Flux<FilePart> parts, ServerWebExchange exchange);

    Mono<Void> getFileById(ServerWebExchange exchange);
}
