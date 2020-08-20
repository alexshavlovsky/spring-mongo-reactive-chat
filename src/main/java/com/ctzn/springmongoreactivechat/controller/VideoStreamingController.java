package com.ctzn.springmongoreactivechat.controller;

import com.ctzn.springmongoreactivechat.domain.CompoundWebVideo;
import com.ctzn.springmongoreactivechat.repository.CompoundWebVideoRepository;
import com.ctzn.springmongoreactivechat.service.attachments.AttachmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static com.ctzn.springmongoreactivechat.service.HttpUtil.getRemoteHost;
import static com.ctzn.springmongoreactivechat.service.HttpUtil.newHttpError;
import static org.springframework.core.io.buffer.DataBufferUtils.skipUntilByteCount;
import static org.springframework.core.io.buffer.DataBufferUtils.takeUntilByteCount;

@RestController
@CrossOrigin
@RequestMapping("/api/videos/")
@Profile("mongo-video-transcoder")
public class VideoStreamingController {

    private Logger LOG = LoggerFactory.getLogger(VideoStreamingController.class);

    private AttachmentService attachmentService;
    private ReactiveMongoOperations mongo;
    private CompoundWebVideoRepository compoundWebVideoRepository;

    public VideoStreamingController(AttachmentService attachmentService, ReactiveMongoOperations mongo, CompoundWebVideoRepository compoundWebVideoRepository) {
        this.attachmentService = attachmentService;
        this.mongo = mongo;
        this.compoundWebVideoRepository = compoundWebVideoRepository;
    }

    private HttpRange parseRange(List<HttpRange> ranges, long contentLength) {
        if (ranges.size() != 0) {
            HttpRange httpRange = ranges.get(0);
            long start = httpRange.getRangeStart(contentLength);
            long end = httpRange.getRangeEnd(contentLength);
            if (start >= 0 && start < contentLength && start <= end) return httpRange;
        }
        return null;
    }

    // This  controller streams video files accepting partial range requests
    @GetMapping("streams/{fileId}")
    public Mono<Void> getVideo(@PathVariable String fileId, ServerWebExchange exchange) {
        HttpHeaders responseHeaders = exchange.getResponse().getHeaders();
        Mono<String> setHeaders = mongo.findOne(new BasicQuery(String.format("{'sources.src' : '%s'}", fileId)), CompoundWebVideo.class)
                .map(compoundWebVideo -> compoundWebVideo.getSources().stream().filter(s -> s.getSrc().equals(fileId)).findAny().get().getType())
                .doOnNext(type -> {
                            responseHeaders.setContentType(MediaType.parseMediaType(type));
                            responseHeaders.set("Accept-Ranges", "bytes");
                        }
                )
                .switchIfEmpty(newHttpError(LOG, exchange, HttpStatus.UNPROCESSABLE_ENTITY, "video", "parent video not found: " + fileId));
        return setHeaders.flatMap(s -> exchange.getResponse().writeWith(attachmentService.getFileLength(fileId)
                .map(contentLength -> {
                    HttpRange range = parseRange(exchange.getRequest().getHeaders().getRange(), contentLength);
                    if (range == null) {
                        responseHeaders.setContentLength(contentLength);
                        return new long[]{0, contentLength - 1, contentLength};
                    } else {
                        exchange.getResponse().setStatusCode(HttpStatus.PARTIAL_CONTENT);
                        long start = range.getRangeStart(contentLength);
                        long end = range.getRangeEnd(contentLength);
                        long rangeLength = end - start + 1;
                        responseHeaders.setContentLength(rangeLength);
                        responseHeaders.set("Content-Range", "bytes " + start + "-" + end + "/" + contentLength);
                        return new long[]{start, end, contentLength};
                    }
                })
                .flatMapMany(range -> {
                    Flux<DataBuffer> content = attachmentService.getAttachmentById(fileId);
                    LOG.info("video->[{}] {} ({}-{})OK", getRemoteHost(exchange), fileId, range[0], range[1]);
                    return takeUntilByteCount(skipUntilByteCount(content, range[0]), range[1] - range[0] + 1);
                })
                .switchIfEmpty(newHttpError(LOG, exchange, HttpStatus.UNPROCESSABLE_ENTITY, "video", "error processing: " + fileId))
        ));
    }

    @GetMapping("sources/{fileId}")
    public Mono<CompoundWebVideo> getCompoundVideoByVideoFileId(@PathVariable String fileId, ServerWebExchange exchange) {
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return compoundWebVideoRepository.findCompoundWebVideoByAttachment_FileId(fileId)
                .doOnNext(compoundWebVideo -> LOG.trace("video-sources->[{}] {}", getRemoteHost(exchange), compoundWebVideo))
                .switchIfEmpty(newHttpError(LOG, exchange, HttpStatus.NOT_FOUND, "video-sources", "source not found: " + fileId));
    }
}
