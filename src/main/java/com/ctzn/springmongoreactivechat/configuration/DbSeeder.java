package com.ctzn.springmongoreactivechat.configuration;

import com.ctzn.springmongoreactivechat.service.messages.MongoBroadcastMessageServiceAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import reactor.core.publisher.Mono;

import static com.ctzn.springmongoreactivechat.service.attachments.MongoUtil.isMongoConnected;

@Configuration
@Profile("mongo-service")
public class DbSeeder {

    @Value("${chat_history_max_entries}")
    int maxDoc;

    @Value("${chat_history_max_size}")
    int maxSize;

    @Value("${chat_history_drop_on_startup}")
    boolean doDropHistory;

    @Value("${test_messages_count}")
    int testMessagesCount;

    @Value("${shutdown_on_db_connection_error}")
    boolean doShutdownOnDbConnectionError;

    private ReactiveMongoOperations mongo;
    private MongoBroadcastMessageServiceAdapter messageService;

    public DbSeeder(ReactiveMongoOperations mongo, MongoBroadcastMessageServiceAdapter messageService) {
        this.mongo = mongo;
        this.messageService = messageService;
    }

    private <T> Mono<Void> dropIfExists(String name) {
        return mongo.collectionExists(name)
                .flatMap(e -> e ? mongo.dropCollection(name) : Mono.empty());
    }

    private <T> Mono<Void> createIfAbsents(String name, CollectionOptions options) {
        return mongo.collectionExists(name)
                .flatMap(e -> e ? Mono.empty() : mongo.createCollection(name, options).then());
    }


    @Bean
    CommandLineRunner startup() {
        return args -> {
            try {
                Boolean connected = isMongoConnected(mongo).block();
                if (connected != null && connected) {
                    CollectionOptions options = CollectionOptions.empty().capped().size(maxSize).maxDocuments(maxDoc);
                    String cappedCollection = "messagesCapped";
                    String persistedCollection = "messagesPersisted";
                    Mono.just(doDropHistory)
                            .flatMap(d -> d ? dropIfExists(cappedCollection).then(dropIfExists(persistedCollection)) : Mono.empty())
                            .switchIfEmpty(createIfAbsents(cappedCollection, options).then(createIfAbsents(persistedCollection, CollectionOptions.empty())))
                            .switchIfEmpty(
                                    MessageSeeder.getInitSequence(testMessagesCount).concatMap(m -> messageService.saveMessage(m)).then()
                            ).block();
                } else
                    throw new RuntimeException(new Exception("Mongo is not responding. To disable this error set the property shutdown_on_db_connection_error = false"));
            } catch (Exception e) {
                if (doShutdownOnDbConnectionError) throw new Error(e.getCause());
            }
        };
    }
}
