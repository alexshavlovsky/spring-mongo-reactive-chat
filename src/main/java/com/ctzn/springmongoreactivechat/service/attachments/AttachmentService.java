package com.ctzn.springmongoreactivechat.service.attachments;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.Map;
import java.util.function.Function;

import static com.ctzn.springmongoreactivechat.service.HttpUtil.getRemoteHost;
import static com.ctzn.springmongoreactivechat.service.HttpUtil.newHttpError;
import static com.ctzn.springmongoreactivechat.service.attachments.MongoUtil.logDownloadProgress;

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

    // saves an attachment and returns a tuple in which
    // the first element T1 contains frontend fileId (a FormData filename actually)
    // and the second element T2 contains fileId returned by an attachment service implementation
    // so that this attachment can be addressed by this id and then this id can be stored in a ReachMessage
    // object on the frontend side
    abstract Function<Flux<FilePart>, Publisher<Tuple2<String, String>>> saveAttachmentsHandler();

    public abstract Mono<String> store(Publisher<DataBuffer> content, String filename);

    // loads an attachment by id and returns an id
    private Function<Mono<String>, Publisher<String>> loadAttachmentByIdHandler(ServerWebExchange exchange) {
        return fileIdMono -> fileIdMono.flatMap(
                fileId -> exchange.getResponse()
                        .writeWith(getAttachmentById(fileId)
                                .transform(logDownloadProgress(LOG, exchange, fileId, getBufferSize()))
                                .switchIfEmpty(newHttpError(LOG, exchange, HttpStatus.NOT_FOUND, "file", "File does not exist: " + fileId))
                        ).thenReturn(fileId)
        );
    }

    abstract int getBufferSize();

    public abstract Flux<DataBuffer> getAttachmentById(String fileId);

    public abstract Mono<Long> getFileLength(String fileId);

    // handles all attachments and returns a map containing entries of a FormData filename and a file id returned by an attachment service
    public Mono<Map<String, String>> saveAttachments(Flux<FilePart> parts, ServerWebExchange exchange) {
        return parts
                .transform(saveAttachmentsHandler())
                .doOnNext(tuple -> LOG.info("<-file[{}] {} as {}", getRemoteHost(exchange), tuple.getT1(), tuple.getT2()))
                .collectMap(Tuple2::getT1, Tuple2::getT2);
    }

    // returns a request parameter by key or an error
    private Mono<String> parseRequestParameterByKey(ServerWebExchange exchange, String key) {
        return exchange.getFormData().flatMap(formData -> formData.containsKey(key) ?
                Mono.just(formData.getFirst(key)) :
                newHttpError(LOG, exchange, HttpStatus.BAD_REQUEST, "", "Form field is required: " + key)
        );
    }

    // parses a file id and dispatches a request
    public Mono<Void> loadAttachmentById(ServerWebExchange exchange) {
        return parseRequestParameterByKey(exchange, FORM_DATA_FILE_ID_KEY)
                .transform(loadAttachmentByIdHandler(exchange))
                .doOnNext(fileId -> LOG.info("file->[{}] {} OK", getRemoteHost(exchange), fileId))
                .then();
    }
}
