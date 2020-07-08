package com.ctzn.springmongoreactivechat.service.thumbs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.schild.jave.*;

import java.io.*;
import java.util.ArrayList;

// This class is a temporal substitute until issues are resolved in the JAVE2 repo
class ScreenExtractorTmp {

    static class RBufferedReader extends BufferedReader {
        private final ArrayList<String> lines = new ArrayList<>();

        RBufferedReader(Reader in) {
            super(in);
        }

        @Override
        public String readLine() throws IOException {
            if (lines.size() > 0) {
                return lines.remove(0);
            } else {
                return super.readLine();
            }
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(ScreenExtractorTmp.class);

    private final FFMPEGLocator locator;

    ScreenExtractorTmp(FFMPEGLocator locator) {
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

        FFMPEGExecutor ffmpeg = this.locator.createExecutor();
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
            RBufferedReader reader = new RBufferedReader(
                    new InputStreamReader(ffmpeg.getErrorStream()));
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
