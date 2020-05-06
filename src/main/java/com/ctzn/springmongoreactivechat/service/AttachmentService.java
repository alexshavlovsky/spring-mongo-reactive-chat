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

    // returns an int array that contains respectively the total storage size in MB and the number of stored files
    abstract public Mono<int[]> getStorageSize();

    // logs storage statistics
    void logStorageStat() {
        getStorageSize().subscribe(a ->
                LOG.info("Total attachments stored: {} MB in {} files", a[0], a[1]));
    }

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
        // TODO:
        //  Issue: file ids do not mapped properly on frontend side
        //  Description: incoming files may be handled in parallel so order of file ids
        //  in resulting Flux may be inconsistent with order of file parts in the request
        //  Solution: returned file id must be mapped with original file index
        //  to properly map file ids on frontend side while building message with
        //  attachments
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
