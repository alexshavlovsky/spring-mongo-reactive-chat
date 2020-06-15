package com.ctzn.springmongoreactivechat.controller;

import com.ctzn.springmongoreactivechat.service.AttachmentService;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping("/files/")
public class AttachmentController {

    private AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, String>> saveAttachments(@RequestPart("file") Flux<FilePart> parts, ServerWebExchange exchange) {
        return attachmentService.saveAttachments(parts, exchange);
    }

    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<Void> getFileById(ServerWebExchange exchange) {
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_OCTET_STREAM);
        return attachmentService.loadAttachmentById(exchange);
    }

    @GetMapping("/thumbs/{fileId}")
    public Mono<Void> getImageThumbByFileId(@PathVariable String fileId, ServerWebExchange exchange) {
        exchange.getResponse().getHeaders().setContentType(MediaType.IMAGE_JPEG);
        return exchange.getResponse().writeWith(
                DataBufferUtils.join(attachmentService.getAttachmentById(fileId)).flatMapMany(dataBuffer -> {
                            ByteArrayOutputStream arrayBuffer = new ByteArrayOutputStream();
                            try {
                                Thumbnails.of(dataBuffer.asInputStream(true))
                                        .size(160, 160).outputFormat("jpg").toOutputStream(arrayBuffer);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            return DataBufferUtils.readInputStream(() -> new ByteArrayInputStream(arrayBuffer.toByteArray()), new DefaultDataBufferFactory(), 64 * 1024);
                        }
                )
        );
    }
}
