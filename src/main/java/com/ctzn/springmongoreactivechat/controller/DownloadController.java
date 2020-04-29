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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.io.File;
import java.nio.file.Paths;

@RestController
@CrossOrigin
public class DownloadController {

    private Logger LOG = LoggerFactory.getLogger(DownloadController.class);

    @GetMapping("/files/{fileName}")
    public Mono<Void> downloadByWriteWith(@PathVariable String fileName, ServerHttpResponse response, ServerHttpRequest request) {
        HttpEvent event = HttpEvent.fromServerHttpRequest(request);
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
}
