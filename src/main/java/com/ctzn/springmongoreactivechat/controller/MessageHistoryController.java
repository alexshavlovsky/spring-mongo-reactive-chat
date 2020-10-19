package com.ctzn.springmongoreactivechat.controller;

import com.ctzn.springmongoreactivechat.domain.Message;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;

import java.util.Comparator;

import static com.ctzn.springmongoreactivechat.service.HttpUtil.getRemoteHost;

@RestController
@CrossOrigin
@RequestMapping("/api/message-history/")
@Profile("mongo-service")
public class MessageHistoryController {

    private Logger LOG = LoggerFactory.getLogger(MessageHistoryController.class);

    private final ReactiveMongoOperations mongo;

    public MessageHistoryController(ReactiveMongoOperations mongo) {
        this.mongo = mongo;
    }

    @GetMapping("{messageId}")
    Flux<Message> getMessagesBefore(@PathVariable String messageId, ServerWebExchange exchange) {
        LOG.info("hist->[{}] {}", getRemoteHost(exchange), messageId);
        return mongo
                .find(Query
                        .query(Criteria.where("id").lt(new ObjectId(messageId)))
                        .with(Sort.by(Sort.Direction.DESC, "id"))
                        .limit(10), Message.class, "messagesPersisted")
                .sort(Comparator.comparing(Message::getId));
    }
}
