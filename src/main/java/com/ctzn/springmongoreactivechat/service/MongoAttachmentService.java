package com.ctzn.springmongoreactivechat.service;

import org.reactivestreams.Publisher;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsResource;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsTemplate;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.function.Function;

import static com.ctzn.springmongoreactivechat.service.MongoUtil.countDocuments;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@Service
@Profile("mongo-grid-attachments")
public class MongoAttachmentService extends AttachmentService {

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
    Function<Flux<FilePart>, Publisher<Tuple2<String, String>>> saveAttachmentsHandler() {
        return parts -> parts.flatMap(part -> gridFsTemplate
                .store(part.content(), part.filename())
                .map(objectId -> Tuples.of(part.filename(), objectId.toHexString()))
        );
    }

    @Override
    int getBufferSize() {
        return 256 * 1024;
    }

    @Override
    public Flux<DataBuffer> getAttachmentById(String fileId) {
        return gridFsTemplate
                .findOne(query(where("_id").is(fileId)))
                .flatMap(gridFsTemplate::getResource)
                .flatMapMany(ReactiveGridFsResource::getDownloadStream);
    }
}
