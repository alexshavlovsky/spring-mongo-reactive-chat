package com.ctzn.springmongoreactivechat.service.attachments;

import com.ctzn.springmongoreactivechat.service.FileUtil;
import org.reactivestreams.Publisher;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.function.Function;

@Service
@Profile("file-system-attachments")
public class FileSystemAttachmentService extends AttachmentService {

    private final String STORAGE_FOLDER_PATH = "app_uploaded_files";
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

    private String getRandomId() {
        return UUID.randomUUID().toString();
    }

    @Override
    Function<Flux<FilePart>, Publisher<Tuple2<String, String>>> saveAttachmentsHandler() {
        return parts -> parts.flatMap(part -> {
            String fileId = getRandomId();
            return part.transferTo(uploadPath.resolve(fileId)).thenReturn(Tuples.of(part.filename(), fileId));
        });
    }

    @Override
    public Mono<String> store(Publisher<DataBuffer> content, String filename) {
        String fileId = getRandomId();
        return FileUtil.transferDataBufferTo(content, uploadPath.resolve(fileId)).thenReturn(fileId);
    }

    @Override
    int getBufferSize() {
        return 64 * 1024;
    }

    @Override
    public Flux<DataBuffer> getAttachmentById(String fileId) {
        Path path = uploadPath.resolve(fileId);
        return path.toFile().exists() ? DataBufferUtils.read(path, new DefaultDataBufferFactory(), getBufferSize()) : Flux.empty();
    }

    @Override
    public Mono<Long> getFileLength(String fileId) {
        return Mono.just(uploadPath.resolve(fileId).toFile().length());
    }
}
