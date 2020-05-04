package com.ctzn.springmongoreactivechat.service;

import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsTemplate;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Function;

import static com.ctzn.springmongoreactivechat.service.HttpUtil.getRemoteHost;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@Service
@Profile("mongo-grid-attachments")
public class MongoAttachmentService implements AttachmentService {

    private Logger LOG = LoggerFactory.getLogger(MongoAttachmentService.class);

    private ReactiveGridFsTemplate gridFsTemplate;

    public MongoAttachmentService(ReactiveGridFsTemplate gridFsTemplate) {
        this.gridFsTemplate = gridFsTemplate;
    }

    private Function<Flux<FilePart>, Publisher<String>> saveFiles = parts -> parts
            .flatMap(part -> gridFsTemplate.store(part.content(), part.filename()))
            .map(ObjectId::toHexString);

    @Override
    public Mono<List<String>> saveAttachments(Flux<FilePart> parts, ServerWebExchange exchange) {
        return HttpUtil.savePartsAndCollectIds(LOG, parts, exchange, saveFiles);
    }

    @Override
    public Mono<Void> getFileById(ServerWebExchange exchange) {
        return HttpUtil.formDataParseFileId(exchange, LOG).flatMap(fileId -> gridFsTemplate
                .findOne(query(where("_id").is(fileId)))
                .flatMap(gridFsTemplate::getResource)
                .flatMap(resource -> exchange.getResponse().writeWith(resource.getDownloadStream()))
                .doOnSuccess(v -> LOG.info("f->[{}] {}", getRemoteHost(exchange), fileId))
        );
    }
}
