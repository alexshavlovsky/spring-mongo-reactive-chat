package com.ctzn.springmongoreactivechat.service.thumbs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.process.ProcessLocator;
import ws.schild.jave.process.ProcessWrapper;
import ws.schild.jave.utils.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

// This class is a temporal substitute until issues are resolved in the JAVE2 repo
class ScreenExtractorTmp {

    private static final Logger LOG = LoggerFactory.getLogger(ScreenExtractorTmp.class);

    private final ProcessLocator locator;

    ScreenExtractorTmp(ProcessLocator locator) {
        this.locator = locator;
    }

    void renderOneImage(MultimediaObject multimediaObject,
                        int width, int height,
                        long millis,
                        File outputFile,
                        int quality,
                        boolean keyframesSeeking)
            throws Exception {
        String inputSource = multimediaObject.isURL() ? multimediaObject.getURL().toString() : multimediaObject.getFile().getAbsolutePath();
        try {
            if (outputFile.exists()) {
                outputFile.delete();
            }
            if (!multimediaObject.isURL() && !multimediaObject.getFile().canRead()) {
                LOG.debug("Failed to open input file");
                throw new SecurityException();
            }
        } catch (SecurityException e) {
            LOG.debug("Access denied checking destination folder", e);
        }

        ProcessWrapper ffmpeg = locator.createExecutor();
        if (keyframesSeeking) {
            ffmpeg.addArgument("-ss");
            ffmpeg.addArgument(Utils.buildTimeDuration(millis));
        }
        ffmpeg.addArgument("-i");
        ffmpeg.addArgument(inputSource);
        if (!keyframesSeeking) {
            ffmpeg.addArgument("-ss");
            ffmpeg.addArgument(Utils.buildTimeDuration(millis));
        }
        ffmpeg.addArgument("-vframes");
        ffmpeg.addArgument("1");
        if (width != -1) {
            ffmpeg.addArgument("-s");
            ffmpeg.addArgument(String.format("%sx%s", String.valueOf(width), String.valueOf(height)));
        }
        ffmpeg.addArgument("-qscale");
        ffmpeg.addArgument(String.valueOf(quality));
        ffmpeg.addArgument(outputFile.getAbsolutePath());
        try {
            ffmpeg.execute();
        } catch (IOException e) {
            throw new Exception(e);
        }
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(ffmpeg.getErrorStream()));
            int lineNR = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                lineNR++;
                LOG.debug("Input Line ({}): {}", lineNR, line);
            }
        } catch (IOException e) {
            throw new Exception(e);
        } finally {
            ffmpeg.destroy();
        }
    }
}
