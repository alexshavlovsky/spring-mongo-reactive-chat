package com.ctzn.springmongoreactivechat.controller;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ZeroCopyHttpOutputMessage;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

@RestController
@CrossOrigin
public class DownloadController {

    @GetMapping("/files/{fileName}")
    public Mono<Void> downloadByWriteWith(@PathVariable String fileName, ServerHttpResponse response) throws IOException {
        ZeroCopyHttpOutputMessage zeroCopyResponse = (ZeroCopyHttpOutputMessage) response;
        Resource resource = new FileSystemResource(Paths.get("uploaded_files").resolve(fileName));
        File file = resource.getFile();
        return zeroCopyResponse.writeWith(file, 0, file.length());
    }

}
