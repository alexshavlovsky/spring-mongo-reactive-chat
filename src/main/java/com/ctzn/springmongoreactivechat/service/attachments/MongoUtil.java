package com.ctzn.springmongoreactivechat.service.attachments;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.time.Duration;
import java.util.function.Function;

import static com.ctzn.springmongoreactivechat.service.HttpUtil.getRemoteHost;

public class MongoUtil {
    public static Mono<Boolean> isMongoConnected(ReactiveMongoOperations mongo) {
        return mongo.executeCommand("{ serverStatus: 1 }")
                .map(d -> true)
                .timeout(Duration.ofSeconds(5), Mono.just(false))
                .defaultIfEmpty(false);
    }

    // only emits 'true' if mongo is connected otherwise emits onComplete
    static Mono<Boolean> isMongoConnectedSilent(ReactiveMongoOperations mongo) {
        return isMongoConnected(mongo)
                .onErrorReturn(false)
                .filter(ok -> ok);
    }

    static Mono<Integer> countDocuments(ReactiveMongoOperations mongo, String collection) {
        return mongo.executeCommand(String.format("{count:'%s'}", collection))
                .map(x -> x.get("n"))
                .cast(Integer.class);
    }

    private static final long SIZE_1MB = 1024 * 1024;
    private static final long LOG_SIZE_INTERVAL = 8 * SIZE_1MB;

    static Function<Flux<DataBuffer>, Publisher<DataBuffer>> logDownloadProgress(Logger log, ServerWebExchange exchange, String fileId, int bufferSize) {
        return x -> x.index()
                .doOnNext(tuple -> {
                    if (tuple.getT1() % (LOG_SIZE_INTERVAL / bufferSize) == 0 && tuple.getT1() != 0)
                        log.info("f->[{}] {} {} MB", getRemoteHost(exchange), fileId, tuple.getT1() / (SIZE_1MB / bufferSize));
                })
                .map(Tuple2::getT2);
    }
}
