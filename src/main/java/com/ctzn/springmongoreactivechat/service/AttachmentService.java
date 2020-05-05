package com.ctzn.springmongoreactivechat.service;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Function;

import static com.ctzn.springmongoreactivechat.service.HttpUtil.getRemoteHost;
import static com.ctzn.springmongoreactivechat.service.HttpUtil.newHttpError;

public abstract class AttachmentService {

    private Logger LOG = LoggerFactory.getLogger(AttachmentService.class);

    static private String FORM_DATA_FILE_ID_KEY = "fileId";

    // saves an attachment and returns an id
    abstract Function<Flux<FilePart>, Publisher<String>> saveAttachmentsHandler();

    // loads an attachment by id and returns an id
    abstract Function<Mono<String>, Publisher<String>> loadAttachmentByIdHandler(ServerWebExchange exchange);

    // handles all attachments and returns a list of ids
    public Mono<List<String>> saveAttachments(Flux<FilePart> parts, ServerWebExchange exchange) {
        return parts
                .transform(saveAttachmentsHandler())
                .doOnNext(fileId -> LOG.info("<-f[{}] {}", getRemoteHost(exchange), fileId))
                .collectList();
    }

    // returns a request parameter by key or an error
    private Mono<String> parseRequestParameterByKey(ServerWebExchange exchange, String key) {
        return exchange.getFormData().flatMap(formData -> formData.containsKey(key) ?
                Mono.just(formData.getFirst(key)) :
                newHttpError(LOG, exchange, HttpStatus.BAD_REQUEST, "Form field is required: " + key)
        );
    }

    // parses a file id and dispatches a request
    public Mono<Void> loadAttachmentById(ServerWebExchange exchange) {
        return parseRequestParameterByKey(exchange, FORM_DATA_FILE_ID_KEY)
                .transform(loadAttachmentByIdHandler(exchange))
                .doOnNext(fileId -> LOG.info("f->[{}] {} OK", getRemoteHost(exchange), fileId))
                .flatMap(fileId -> Mono.empty());
    }
}
