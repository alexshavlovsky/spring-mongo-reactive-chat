package com.ctzn.springmongoreactivechat.service;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ZeroCopyHttpOutputMessage;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static com.ctzn.springmongoreactivechat.service.HttpUtil.getRemoteHost;
import static com.ctzn.springmongoreactivechat.service.HttpUtil.newHttpError;

@Service
@Profile("file-system-attachments")
public class FileSystemAttachmentService implements AttachmentService {

    private Logger LOG = LoggerFactory.getLogger(FileSystemAttachmentService.class);

    private final String STORAGE_FOLDER_PATH = "uploaded_files";
    private final Path uploadPath = Paths.get(STORAGE_FOLDER_PATH);

    public FileSystemAttachmentService() {
        if (!Files.exists(uploadPath)) {
            try {
                Files.createDirectories(uploadPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Function<Flux<FilePart>, Publisher<String>> saveFiles = parts -> parts.flatMap(part -> {
        String fileId = UUID.randomUUID().toString();
        return part.transferTo(uploadPath.resolve(fileId)).then(Mono.just(fileId));
    });

    @Override
    public Mono<List<String>> saveAttachments(Flux<FilePart> parts, ServerWebExchange exchange) {
        return HttpUtil.savePartsAndCollectIds(LOG, parts, exchange, saveFiles);
    }

    @Override
    public Mono<Void> getFileById(ServerWebExchange exchange) {
        return HttpUtil.formDataParseFileId(exchange, LOG).flatMap(fileId -> {
            File file = uploadPath.resolve((String) fileId).toFile();
            if (!file.exists()) return newHttpError(LOG, exchange, HttpStatus.NOT_FOUND,
                    "File does not exist: " + fileId);
            return ((ZeroCopyHttpOutputMessage) exchange.getResponse()).writeWith(file, 0, file.length())
                    .doOnSuccess(v -> LOG.info("f->[{}] {}", getRemoteHost(exchange), fileId));
        });
    }
}
