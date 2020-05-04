package com.ctzn.springmongoreactivechat.service;

import org.bson.types.ObjectId;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

import static com.ctzn.springmongoreactivechat.service.HttpUtil.newHttpError;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@Service
@Profile("mongo-grid-attachments")
public class MongoAttachmentService extends AttachmentService {

    private Logger LOG = LoggerFactory.getLogger(MongoAttachmentService.class);

    private ReactiveGridFsTemplate gridFsTemplate;

    public MongoAttachmentService(ReactiveGridFsTemplate gridFsTemplate) {
        this.gridFsTemplate = gridFsTemplate;
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
                .flatMap(resource -> exchange.getResponse().writeWith(resource.getDownloadStream()).thenReturn(fileId))
                .switchIfEmpty(newHttpError(LOG, exchange, HttpStatus.NOT_FOUND, "File does not exist: " + fileId))
        );
    }
}
