package com.ctzn.springmongoreactivechat.service.ffmpeglocator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.schild.jave.DefaultFFMPEGLocator;
import ws.schild.jave.FFMPEGLocator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

// this custom locator substitutes ffmpeg native executable with version 4.3.0 for windows amd64 platform
// see the line #56
public class CustomFfmpegLocator extends FFMPEGLocator {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultFFMPEGLocator.class);

    /**
     * Trace the version of the bundled ffmpeg executable. It's a counter: every
     * time the bundled ffmpeg change it is incremented by 1.
     */
    private static final String MY_EXE_VERSION = "2.7.3";

    /**
     * The ffmpeg executable file path.
     */
    private final String path;

    /**
     * It builds the default FFMPEGLocator, exporting the ffmpeg executable on a
     * temp file.
     */
    public CustomFfmpegLocator() {
        String os = System.getProperty("os.name").toLowerCase();
        boolean isWindows = os.contains("windows");
        boolean isMac = os.contains("mac");
        LOG.debug("Os name is <{}> isWindows: {} isMac: {}", os, isWindows, isMac);

        // Dir Folder
        File dirFolder = new File(System.getProperty("java.io.tmpdir"), "jave/");
        if (!dirFolder.exists()) {
            LOG.debug("Creating jave temp folder to place executables in <{}>", dirFolder.getAbsolutePath());
            dirFolder.mkdirs();
        } else {
            LOG.debug("Jave temp folder exists in <{}>", dirFolder.getAbsolutePath());
        }

        // -----------------ffmpeg executable export on disk.-----------------------------
        String suffix = isWindows ? ".exe" : (isMac ? "-osx" : "");
        String arch = System.getProperty("os.arch");

        //File
        File ffmpegFile = new File(dirFolder, "ffmpeg-" + arch + "-" + ((isWindows && "amd64".equals(arch)) ? "4.3.0" : MY_EXE_VERSION) + suffix);
        LOG.debug("Executable path: {}", ffmpegFile.getAbsolutePath());

        //Check the version of existing .exe file
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
                Runtime.getRuntime().exec(new String[]
                        {
                                "/bin/chmod", "755", ffmpegFile.getAbsolutePath()
                        });
            } catch (IOException e) {
                LOG.error("Error setting executable via chmod", e);
            }
        }

        // Everything seems okay
        path = ffmpegFile.getAbsolutePath();
        LOG.debug("ffmpeg executable found: {}", path);
    }

    @Override
    public String getFFMPEGExecutablePath() {
        return path;
    }

    /**
     * Copies a file bundled in the package to the supplied destination.
     *
     * @param path The name of the bundled file.
     * @param dest The destination.
     * @throws RuntimeException If an unexpected error occurs.
     */
    private void copyFile(String path, File dest) {
        String resourceName = "nativebin/" + path;
        try {
            LOG.debug("Copy from resource <{}> to target <{}>", resourceName, dest.getAbsolutePath());
            InputStream is = getClass().getResourceAsStream(resourceName);
            if (is == null) {
                // Use this for Java 9+ only if required
                resourceName = "ws/schild/jave/nativebin/" + path;
                LOG.debug("Alternative copy from SystemResourceAsStream <{}> to target <{}>", resourceName, dest.getAbsolutePath());
                is = ClassLoader.getSystemResourceAsStream(resourceName);
            }
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
            LOG.error("Could not find ffmpeg executable for {} is the correct platform jar included?", resourceName);
            throw ex;
        }
    }

    /**
     * Copy a file from source to destination.
     *
     * @param source      The name of the bundled file.
     * @param destination the destination
     * @return True if succeeded , False if not
     */
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
}
