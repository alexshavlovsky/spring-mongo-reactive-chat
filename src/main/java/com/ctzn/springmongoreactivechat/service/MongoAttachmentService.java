package com.ctzn.springmongoreactivechat.service;

import org.bson.types.ObjectId;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

import static com.ctzn.springmongoreactivechat.service.HttpUtil.newHttpError;
import static com.ctzn.springmongoreactivechat.service.MongoUtil.countDocuments;
import static com.ctzn.springmongoreactivechat.service.MongoUtil.logDownloadProgress;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@Service
@Profile("mongo-grid-attachments")
public class MongoAttachmentService extends AttachmentService {

    private Logger LOG = LoggerFactory.getLogger(MongoAttachmentService.class);

    private ReactiveGridFsTemplate gridFsTemplate;
    private ReactiveMongoOperations mongo;

    public MongoAttachmentService(ReactiveGridFsTemplate gridFsTemplate, ReactiveMongoOperations mongo) {
        this.gridFsTemplate = gridFsTemplate;
        this.mongo = mongo;
        logStorageStat();
    }

    @Override
    public Mono<int[]> getStorageSize() {
        return MongoUtil.isMongoConnectedSilent(mongo).flatMap(ok ->
                Mono.zip(
                        countDocuments(mongo, "attachments.chunks").map(n -> n / 4),
                        countDocuments(mongo, "attachments.files")
                ).map(t -> t.toList().stream().mapToInt(x -> (int) x).toArray()));
    }

    @Override
    Function<Flux<FilePart>, Publisher<String>> saveAttachmentsHandler() {
        return parts -> parts
                .flatMap(part -> gridFsTemplate.store(part.content(), part.filename()))
                .map(ObjectId::toHexString);
    }

    @Override
    Function<Mono<String>, Publisher<String>> loadAttachmentByIdHandler(ServerWebExchange exchange) {
        return fileIdMono -> fileIdMono.flatMap(fileId -> gridFsTemplate
                .findOne(query(where("_id").is(fileId)))
                .flatMap(gridFsTemplate::getResource)
                .flatMap(resource -> exchange.getResponse().writeWith(resource.getDownloadStream()
                        .transform(logDownloadProgress(LOG, exchange, fileId, 256 * 1024))
                ).thenReturn(fileId))
                .switchIfEmpty(newHttpError(LOG, exchange, HttpStatus.NOT_FOUND, "File does not exist: " + fileId))
        );
    }
}
