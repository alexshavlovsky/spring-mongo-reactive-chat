package com.ctzn.springmongoreactivechat.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtil {
    static private final String TEMP_FOLDER_PATH = "app_temp_folder";
    static private final Path tempPath = Paths.get(TEMP_FOLDER_PATH);

    static private void createTempFolder() throws IOException {
        if (!Files.exists(tempPath)) Files.createDirectories(tempPath);
    }

    static public Path getTempFolder() throws IOException {
        createTempFolder();
        return tempPath;
    }
}
