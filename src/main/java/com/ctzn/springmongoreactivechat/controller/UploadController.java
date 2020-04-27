package com.ctzn.springmongoreactivechat.controller;

import com.ctzn.springmongoreactivechat.domain.HttpEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@CrossOrigin
@RestController
public class UploadController {

    private Logger LOG = LoggerFactory.getLogger(UploadController.class);

    private final Path uploadPath;

    public UploadController() {
        uploadPath = Paths.get("uploaded_files");
        if (!Files.exists(uploadPath)) {
            try {
                Files.createDirectories(uploadPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @PostMapping(value = "/files/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public Mono<String> process(@RequestPart("file") Flux<FilePart> parts, ServerHttpRequest request) {
        HttpEvent event = HttpEvent.fromServerHttpRequest(request);
        return parts.flatMap(part -> part.transferTo(uploadPath.resolve(part.filename())).then(Mono.just(part)))
                .doOnNext(part -> LOG.info("<-f[{}] {}", event.getRemoteHost(), part.filename()))
                .count().map(Object::toString);
    }
}
