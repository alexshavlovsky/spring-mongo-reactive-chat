package com.ctzn.springmongoreactivechat.service;

import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.*;

public class FileUtil {
    static private final String TEMP_FOLDER_PATH = "app_temp_folder";
    static private final Path tempPath = Paths.get(TEMP_FOLDER_PATH);

    static public void createFolder(Path path) throws IOException {
        if (!Files.exists(path)) Files.createDirectories(path);
    }

    static public Path getTempFolder() throws IOException {
        createFolder(tempPath);
        return tempPath;
    }

    private static final OpenOption[] FILE_CHANNEL_OPTIONS =
            {StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE};

    static public Mono<Void> transferDataBufferTo(Publisher<DataBuffer> content, Path dest) {
        return DataBufferUtils.write(content, dest, FILE_CHANNEL_OPTIONS);
    }
}


