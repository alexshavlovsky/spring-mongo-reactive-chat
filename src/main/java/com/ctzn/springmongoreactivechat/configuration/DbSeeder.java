package com.ctzn.springmongoreactivechat.configuration;

import com.ctzn.springmongoreactivechat.domain.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import reactor.core.publisher.Mono;

@Configuration
public class DbSeeder {

    @Value("${chat_history_max_entries}")
    int maxDoc;

    @Value("${chat_history_max_size}")
    int maxSize;

    @Value("${chat_history_drop_on_startup}")
    boolean doDropHistory;


    private ReactiveMongoOperations mongo;

    public DbSeeder(ReactiveMongoOperations mongo) {
        this.mongo = mongo;
    }

    private <T> Mono<Void> dropIfExists(Class<T> clazz) {
        return mongo.collectionExists(clazz)
                .flatMap(e -> e ? mongo.dropCollection(clazz) : Mono.empty());
    }

    private <T> Mono<Void> createIfAbsents(Class<T> clazz, CollectionOptions options) {
        return mongo.collectionExists(clazz)
                .flatMap(e -> e ? Mono.empty() : mongo.createCollection(Message.class, options).then());
    }


    @Bean
    CommandLineRunner startup() {
        return args -> {
            CollectionOptions options = CollectionOptions.empty().capped().size(maxSize).maxDocuments(maxDoc);

            Mono.just(doDropHistory)
                    .flatMap(d -> d ? dropIfExists(Message.class) : Mono.empty())
                    .switchIfEmpty(createIfAbsents(Message.class, options))
                    .switchIfEmpty(mongo.save(Message.newInfo("Service started")).then()).block();
        };
    }
}
