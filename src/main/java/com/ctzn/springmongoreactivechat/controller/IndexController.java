package com.ctzn.springmongoreactivechat.controller;

import com.ctzn.springmongoreactivechat.domain.HttpEvent;
import com.ctzn.springmongoreactivechat.repository.HttpEventRepository;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {

    private HttpEventRepository httpEventRepository;

    public IndexController(HttpEventRepository httpEventRepository) {
        this.httpEventRepository = httpEventRepository;
    }

    @GetMapping("/")
    public String index(ServerHttpRequest request) {
        httpEventRepository.save(HttpEvent.fromServerHttpRequest(request)).subscribe(System.out::println);
        return "index";
    }
}
