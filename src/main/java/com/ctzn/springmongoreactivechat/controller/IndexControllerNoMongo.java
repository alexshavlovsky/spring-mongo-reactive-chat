package com.ctzn.springmongoreactivechat.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Profile("replay-service")
public class IndexControllerNoMongo {
    @GetMapping("/")
    public String index(ServerHttpRequest request) {
        return "index";
    }
}
