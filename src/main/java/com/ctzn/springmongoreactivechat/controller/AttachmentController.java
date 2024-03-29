package com.ctzn.springmongoreactivechat.controller;

import com.ctzn.springmongoreactivechat.service.attachments.AttachmentService;
import com.ctzn.springmongoreactivechat.service.thumbs.ThumbsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

import static com.ctzn.springmongoreactivechat.service.HttpUtil.getRemoteHost;
import static com.ctzn.springmongoreactivechat.service.HttpUtil.newHttpError;

@RestController
@CrossOrigin
@RequestMapping("/api/files/")
public class AttachmentController {

    private Logger LOG = LoggerFactory.getLogger(AttachmentController.class);

    private AttachmentService attachmentService;
    private ThumbsService thumbsService;

    public AttachmentController(AttachmentService attachmentService,
                                @Qualifier("caffeine-caching-proxy") ThumbsService thumbsService) {
        this.attachmentService = attachmentService;
        this.thumbsService = thumbsService;
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

    @GetMapping("thumbs/{thumbType}/{fileId}")
    public Mono<Void> getImageThumbByFileId(@PathVariable String fileId, @PathVariable String
            thumbType, ServerWebExchange exchange) {
        exchange.getResponse().getHeaders().setContentType(thumbsService.getMediaType());
        return exchange.getResponse().writeWith(
                thumbsService.getThumb(new ThumbsService.ThumbKey(fileId, thumbType))
                        .map(bytes -> exchange.getResponse().bufferFactory().wrap(bytes))
                        .doOnNext(dataBuffer -> LOG.info("thumb->[{}] {} OK", getRemoteHost(exchange), fileId))
                        .onErrorResume(e -> newHttpError(LOG, exchange, HttpStatus.UNPROCESSABLE_ENTITY, "thumb", e.getMessage()))
        );
    }
}
