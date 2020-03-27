package com.ctzn.springmongoreactivechat.configuration;

import com.ctzn.springmongoreactivechat.domain.Message;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import reactor.core.publisher.Mono;

@Configuration
public class DbSeeder {

    private ReactiveMongoOperations mongo;

    public DbSeeder(ReactiveMongoOperations mongo) {
        this.mongo = mongo;
    }

    private <T> Mono<Void> dropIfExists(Class<T> clazz) {
        return mongo.collectionExists(clazz)
                .flatMap(e -> e ? mongo.dropCollection(clazz) : Mono.empty());
    }

    @Bean
    CommandLineRunner startup() {
        return args -> {
            CollectionOptions options = CollectionOptions.empty().capped().size(1024 * 1024).maxDocuments(100);

            dropIfExists(Message.class)
                    .switchIfEmpty(mongo.createCollection(Message.class, options).then())
                    .switchIfEmpty(mongo.save(Message.newInfo("Service started")).then()).block();
        };
    }
}
