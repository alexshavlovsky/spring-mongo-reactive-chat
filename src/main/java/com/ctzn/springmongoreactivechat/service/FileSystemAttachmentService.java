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
import java.util.UUID;
import java.util.function.Function;

import static com.ctzn.springmongoreactivechat.service.HttpUtil.newHttpError;

@Service
@Profile("file-system-attachments")
public class FileSystemAttachmentService extends AttachmentService {

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

    @Override
    Function<Flux<FilePart>, Publisher<String>> saveAttachmentsHandler() {
        return parts -> parts.flatMap(part -> {
            String fileId = UUID.randomUUID().toString();
            return part.transferTo(uploadPath.resolve(fileId)).then(Mono.just(fileId));
        });
    }

    @Override
    Function<Mono<String>, Publisher<String>> loadAttachmentByIdHandler(ServerWebExchange exchange) {
        return fileIdMono -> fileIdMono.flatMap(fileId -> {
            File file = uploadPath.resolve(fileId).toFile();
            return file.exists() ?
                    ((ZeroCopyHttpOutputMessage) exchange.getResponse()).writeWith(file, 0, file.length()).thenReturn(fileId) :
                    newHttpError(LOG, exchange, HttpStatus.NOT_FOUND, "File does not exist: " + fileId);
        });
    }
}
