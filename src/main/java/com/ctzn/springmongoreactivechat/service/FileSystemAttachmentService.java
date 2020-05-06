package com.ctzn.springmongoreactivechat.service;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import java.util.function.Function;

import static com.ctzn.springmongoreactivechat.service.HttpUtil.newHttpError;
import static com.ctzn.springmongoreactivechat.service.MongoUtil.logDownloadProgress;

@Service
@Profile("file-system-attachments")
public class FileSystemAttachmentService extends AttachmentService {

    private Logger LOG = LoggerFactory.getLogger(FileSystemAttachmentService.class);

    private final String STORAGE_FOLDER_PATH = "uploaded_files";
    private final int DATA_BUFFER_SIZE = 64 * 1024;
    private final Path uploadPath = Paths.get(STORAGE_FOLDER_PATH);

    public FileSystemAttachmentService() {
        if (!Files.exists(uploadPath)) {
            try {
                Files.createDirectories(uploadPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        logStorageStat();
    }

    @Override
    public Mono<int[]> getStorageSize() {
        try {
            return Flux.fromStream(Files.list(uploadPath))
                    .filter(p -> p.toFile().isFile())
                    .map(p -> p.toFile().length())
                    .reduce(new int[]{0, 0}, (a, v) -> {
                        a[0] += v / 1048576;
                        a[1]++;
                        return a;
                    });
        } catch (IOException e) {
            e.printStackTrace();
            return Mono.just(new int[]{0, 0});
        }
    }

    @Override
    Function<Flux<FilePart>, Publisher<Tuple2<String, String>>> saveAttachmentsHandler() {
        return parts -> parts.flatMap(part -> {
            String fileId = UUID.randomUUID().toString();
            return part.transferTo(uploadPath.resolve(fileId)).then(Mono.just(Tuples.of(part.filename(), fileId)));
        });
    }

    @Override
    Function<Mono<String>, Publisher<String>> loadAttachmentByIdHandler(ServerWebExchange exchange) {
        return fileIdMono -> fileIdMono.flatMap(fileId -> {
            Path path = uploadPath.resolve(fileId);
            return path.toFile().exists() ?
                    exchange.getResponse().writeWith(
                            DataBufferUtils.read(path, new DefaultDataBufferFactory(), DATA_BUFFER_SIZE, StandardOpenOption.READ)
                                    .transform(logDownloadProgress(LOG, exchange, fileId, DATA_BUFFER_SIZE))
                    ).thenReturn(fileId) :
//                    ((ZeroCopyHttpOutputMessage) exchange.getResponse()).writeWith(file, 0, file.length()).thenReturn(fileId) :
                    newHttpError(LOG, exchange, HttpStatus.NOT_FOUND, "File does not exist: " + fileId);
        });
    }
}
