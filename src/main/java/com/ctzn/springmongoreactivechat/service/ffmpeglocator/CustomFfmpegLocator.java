package com.ctzn.springmongoreactivechat.service.ffmpeglocator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.schild.jave.Version;
import ws.schild.jave.process.ProcessLocator;
import ws.schild.jave.process.ProcessWrapper;
import ws.schild.jave.process.ffmpeg.FFMPEGProcess;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class CustomFfmpegLocator implements ProcessLocator {
    private static final Logger LOG = LoggerFactory.getLogger(CustomFfmpegLocator.class);

    private final String path;

    public CustomFfmpegLocator() {
        String os = System.getProperty("os.name").toLowerCase();
        boolean isWindows = os.contains("windows");
        boolean isMac = os.contains("mac");
        LOG.debug("Os name is <{}> isWindows: {} isMac: {}", os, isWindows, isMac);

        // Dir Folder
        File dirFolder = new File(System.getProperty("java.io.tmpdir"), "jave/");
        if (!dirFolder.exists()) {
            LOG.debug(
                    "Creating jave temp folder to place executables in <{}>", dirFolder.getAbsolutePath());
            dirFolder.mkdirs();
        } else {
            LOG.debug("Jave temp folder exists in <{}>", dirFolder.getAbsolutePath());
        }

        // -----------------ffmpeg executable export on disk.-----------------------------
        String suffix = isWindows ? ".exe" : (isMac ? "-osx" : "");
        String arch = System.getProperty("os.arch");

        // File
        File ffmpegFile = new File(dirFolder, "ffmpeg-" + arch + "-" + Version.getVersion() + suffix);
        LOG.debug("Executable path: {}", ffmpegFile.getAbsolutePath());

        // Check the version of existing .exe file
        if (ffmpegFile.exists()) {
            // OK, already present
            LOG.debug("Executable exists in <{}>", ffmpegFile.getAbsolutePath());
        } else {
            LOG.debug("Need to copy executable to <{}>", ffmpegFile.getAbsolutePath());
            copyFile("ffmpeg-" + arch + suffix, ffmpegFile);
        }

        // Need a chmod?
        if (!isWindows) {
            try {
                Runtime.getRuntime().exec(new String[]{"/bin/chmod", "755", ffmpegFile.getAbsolutePath()});
            } catch (IOException e) {
                LOG.error("Error setting executable via chmod", e);
            }
        }

        // Everything seems okay
        path = ffmpegFile.getAbsolutePath();
        if (ffmpegFile.exists()) {
            LOG.debug("ffmpeg executable found: {}", path);
        } else {
            LOG.error("ffmpeg executable NOT found: {}", path);
        }
    }

    @Override
    public String getExecutablePath() {
        return path;
    }

    private void copyFile(String path, File dest) {
        String resourceName = "nativebin/" + path;
        try {
            LOG.debug("Copy from resource <{}> to target <{}>", resourceName, dest.getAbsolutePath());

            ClassLoader classloader = Thread.currentThread().getContextClassLoader();
            InputStream is = classloader.getResourceAsStream(resourceName);

            if (is != null) {
                if (copy(is, dest.getAbsolutePath())) {
                    if (dest.exists()) {
                        LOG.debug("Target <{}> exists", dest.getAbsolutePath());
                    } else {
                        LOG.error("Target <{}> does not exist", dest.getAbsolutePath());
                    }
                } else {
                    LOG.error("Copy resource to target <{}> failed", dest.getAbsolutePath());
                }
                try {
                    is.close();
                } catch (IOException ioex) {
                    LOG.warn("Error in closing input stream", ioex);
                }
            } else {
                LOG.error("Could not find ffmpeg platform executable in resources for <{}>", resourceName);
            }
        } catch (NullPointerException ex) {
            LOG.error(
                    "Could not find ffmpeg executable for {} is the correct platform jar included?",
                    resourceName);
            throw ex;
        }
    }

    private boolean copy(InputStream source, String destination) {
        boolean success = true;

        try {
            Files.copy(source, Paths.get(destination), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            LOG.error("Cannot write file " + destination, ex);
            success = false;
        }

        return success;
    }

    @Override
    public ProcessWrapper createExecutor() {
        return new FFMPEGProcess(getExecutablePath());
    }
}
