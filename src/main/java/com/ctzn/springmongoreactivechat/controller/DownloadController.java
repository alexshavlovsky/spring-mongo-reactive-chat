package com.ctzn.springmongoreactivechat.controller;

import com.ctzn.springmongoreactivechat.domain.HttpEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.ZeroCopyHttpOutputMessage;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.File;
import java.nio.file.Paths;

@RestController
@CrossOrigin
public class DownloadController {

    private Logger LOG = LoggerFactory.getLogger(DownloadController.class);

    @PostMapping("/files/")
    public Mono<Void> getFileById(ServerWebExchange serverWebExchange, ServerHttpResponse response, ServerHttpRequest request) {
        return serverWebExchange.getFormData().flatMap(formData -> {
                    HttpEvent event = HttpEvent.fromServerHttpRequest(request);
                    String fileName = formData.getFirst("id");
                    if (fileName == null) {
                        LOG.error("f? [{}]", event.getRemoteHost());
                        response.setRawStatusCode(400);
                        return Mono.empty();
                    }
                    ZeroCopyHttpOutputMessage zeroCopyResponse = (ZeroCopyHttpOutputMessage) response;
                    File file = Paths.get("uploaded_files").resolve(fileName).toFile();
                    if (file.exists()) {
                        response.getHeaders().setContentType(MediaType.APPLICATION_OCTET_STREAM);
                        return zeroCopyResponse.writeWith(file, 0, file.length())
                                .doOnSuccess(part -> LOG.info("f->[{}] {}", event.getRemoteHost(), fileName));
                    } else {
                        DataBuffer dataBuffer = response.bufferFactory().allocateBuffer();
                        dataBuffer.write(fileName.getBytes());
                        response.setRawStatusCode(404);
                        response.getHeaders().setContentType(MediaType.TEXT_PLAIN);
                        return zeroCopyResponse.writeWith(Mono.just(dataBuffer))
                                .doOnSuccess(part -> LOG.error("fx [{}] {}", event.getRemoteHost(), fileName));
                    }
                }
        );
    }
}
