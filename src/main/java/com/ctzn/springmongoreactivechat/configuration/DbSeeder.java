package com.ctzn.springmongoreactivechat.configuration;

import com.ctzn.springmongoreactivechat.domain.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Configuration
@Profile("mongo-service")
public class DbSeeder {

    @Value("${chat_history_max_entries}")
    int maxDoc;

    @Value("${chat_history_max_size}")
    int maxSize;

    @Value("${chat_history_drop_on_startup}")
    boolean doDropHistory;

    @Value("${shutdown_on_db_connection_error}")
    boolean doShutdownOnDbConnectionError;

    private ReactiveMongoOperations mongo;

    public DbSeeder(ReactiveMongoOperations mongo) {
        this.mongo = mongo;
    }

    private Mono<Boolean> isMongoConnected() {
        return mongo.executeCommand("{ serverStatus: 1 }")
                .map(d -> true)
                .timeout(Duration.ofSeconds(3), Mono.just(false))
                .onErrorReturn(false)
                .defaultIfEmpty(false);
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
            if (doShutdownOnDbConnectionError) {
                Boolean connected = isMongoConnected().block();
                if (connected == null || !connected) throw new Error("Failed to connect to MongoDB");
            }

            CollectionOptions options = CollectionOptions.empty().capped().size(maxSize).maxDocuments(maxDoc);

            Mono.just(doDropHistory)
                    .flatMap(d -> d ? dropIfExists(Message.class) : Mono.empty())
                    .switchIfEmpty(createIfAbsents(Message.class, options))
                    .switchIfEmpty(mongo.save(Message.newInfo("Service started")).then()).block();
        };
    }
}
