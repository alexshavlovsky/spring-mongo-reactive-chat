package com.ctzn.springmongoreactivechat.service;

import com.ctzn.springmongoreactivechat.domain.Message;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Profile("mongo-service")
public class MongoBroadcastMessageServiceAdapter implements BroadcastMessageService {
    private ReactiveMongoOperations mongo;

    public MongoBroadcastMessageServiceAdapter(ReactiveMongoOperations mongo) {
        this.mongo = mongo;
    }

    @Override
    public Flux<Message> getTopic() {
        return mongo.tail(new BasicQuery("{}"), Message.class);
    }

    @Override
    public Mono<Message> saveMessage(Message message) {
        return mongo.save(message);
    }
}
