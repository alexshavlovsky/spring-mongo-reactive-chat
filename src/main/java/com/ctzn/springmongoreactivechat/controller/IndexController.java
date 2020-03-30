package com.ctzn.springmongoreactivechat.controller;

import com.ctzn.springmongoreactivechat.domain.HttpEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Profile("mongo-service")
public class IndexController {
    private Logger LOG = LoggerFactory.getLogger(IndexController.class);
    private ReactiveMongoOperations mongo;

    public IndexController(ReactiveMongoOperations mongo) {
        this.mongo = mongo;
    }

    @GetMapping("/")
    public String index(ServerHttpRequest request) {
        mongo.save(HttpEvent.fromServerHttpRequest(request)).subscribe(e -> LOG.info(e.toString()));
        return "index";
    }
}
