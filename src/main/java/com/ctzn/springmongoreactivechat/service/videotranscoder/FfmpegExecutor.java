package com.ctzn.springmongoreactivechat.service.videotranscoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.schild.jave.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

// ================== USAGE EXAMPLE ==================

//        File file = new File("c:/vid/111.mp4");
//                MultimediaObject multimediaObject = new MultimediaObject(file);
//                WebTranscoder webTranscoder = new WebTranscoder();
//                webTranscoder.transcode(multimediaObject, file.getAbsoluteFile().getParentFile(), "webm", "1080");
//                webTranscoder.transcode(multimediaObject, file.getAbsoluteFile().getParentFile(), "mp4", "1080");

// resulting files will be located at paths: c:/vid/1080.mp4 and c:/vid/1080.webm

public class FfmpegExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(FfmpegExecutor.class);

    private static final Pattern SUCCESS_PATTERN =
            Pattern.compile("^\\s*video:\\S+\\s+audio:\\S+\\s+subtitle:\\S+\\s+global headers:\\S+.*$",
                    Pattern.CASE_INSENSITIVE);

    private final FFMPEGLocator locator;

    public FfmpegExecutor() {
        this.locator = new DefaultFFMPEGLocator();
    }

    public FfmpegExecutor(FFMPEGLocator locator) {
        this.locator = locator;
    }

    private void encode(MultimediaObject multimediaObject, File targetFolder, String fileName, List<String> args) throws Exception {
        targetFolder = targetFolder.getAbsoluteFile();
        targetFolder.mkdirs();

        FFMPEGExecutor ffmpeg = locator.createExecutor();
        ffmpeg.addArgument("-i");
        if (multimediaObject.isURL()) ffmpeg.addArgument(multimediaObject.getURL().toString());
        else ffmpeg.addArgument(multimediaObject.getFile().getAbsolutePath());
        args.forEach(ffmpeg::addArgument);
        ffmpeg.addArgument("-y");
        if (fileName == null) ffmpeg.addArgument("NUL"); // TODO: add correct output for linux
        else ffmpeg.addArgument(new File(targetFolder, fileName).getAbsolutePath());

        try {
            ffmpeg.execute();
        } catch (IOException e) {
            throw new Exception(e);
        }
        try {
            String lastWarning = null;
            long duration = 0;
            BufferedReader reader = new BufferedReader(new InputStreamReader(ffmpeg.getErrorStream()));
            String line;
            ConversionOutputAnalyzer outputAnalyzer = new ConversionOutputAnalyzer(duration, null);
            while ((line = reader.readLine()) != null) {
                outputAnalyzer.analyzeNewLine(line);
            }
            if (outputAnalyzer.getLastWarning() != null) {
                if (!SUCCESS_PATTERN.matcher(lastWarning).matches()) {
                    throw new Exception("No match for: " + SUCCESS_PATTERN + " in " + lastWarning);
                }
            }
            int exitCode = ffmpeg.getProcessExitCode();
            if (exitCode != 0) {
                LOG.error("Process exit code: {}  to {}", exitCode, targetFolder.getName());
                throw new Exception("Exit code of ffmpeg encoding run is " + exitCode);
            }
        } catch (IOException e) {
            throw new Exception(e);
        } finally {
            ffmpeg.destroy();
        }
    }

    // ================ H264 command lines ====================
    // ffmpeg -i input -passlogfile _0480mp4 -vf scale="trunc(oh*a/2)*2:480" -b:v 750k -minrate 375k -maxrate 1088k -c:v libx264 -pass 1 -an -f mp4 -y NUL
    // ffmpeg -i input -passlogfile _0480mp4 -vf scale="trunc(oh*a/2)*2:480" -b:v 750k -minrate 375k -maxrate 1088k -c:v libx264 -pass 2 -c:a aac -b:a 128k 0480.mp4
    // ffmpeg -i input -passlogfile _0720mp4 -vf scale="trunc(oh*a/2)*2:720" -b:v 1024k -minrate 512k -maxrate 1485k -c:v libx264 -pass 1 -an -f mp4 -y NUL
    // ffmpeg -i input -passlogfile _0720mp4 -vf scale="trunc(oh*a/2)*2:720" -b:v 1024k -minrate 512k -maxrate 1485k -c:v libx264 -pass 2 -c:a aac -b:a 128k 0720.mp4
    // ffmpeg -i input -passlogfile _1080mp4 -vf scale="trunc(oh*a/2)*2:1080" -b:v 1800k -minrate 900k -maxrate 2610k -c:v libx264 -pass 1 -an -f mp4 -y NUL
    // ffmpeg -i input -passlogfile _1080mp4 -vf scale="trunc(oh*a/2)*2:1080" -b:v 1800k -minrate 900k -maxrate 2610k -c:v libx264 -pass 2 -c:a aac -b:a 128k 1080.mp4

    // ================ WEBM command lines ====================
    // ffmpeg -i input -passlogfile _0480web -vf scale="trunc(oh*a/2)*2:480" -b:v 750k -minrate 375k -maxrate 1088k -quality good -crf 33 -c:v libvpx-vp9 -c:a libopus -pass 1 -speed 4    0480.webm
    // ffmpeg -i input -passlogfile _0480web -vf scale="trunc(oh*a/2)*2:480" -b:v 750k -minrate 375k -maxrate 1088k -quality good -crf 33 -c:v libvpx-vp9 -c:a libopus -pass 2 -speed 4 -y 0480.webm
    // ffmpeg -i input -passlogfile _0720web -vf scale="trunc(oh*a/2)*2:720" -b:v 1024k -minrate 512k -maxrate 1485k -quality good -crf 32 -c:v libvpx-vp9 -c:a libopus -pass 1 -speed 4    0720.webm
    // ffmpeg -i input -passlogfile _0720web -vf scale="trunc(oh*a/2)*2:720" -b:v 1024k -minrate 512k -maxrate 1485k -quality good -crf 32 -c:v libvpx-vp9 -c:a libopus -pass 2 -speed 4 -y 0720.webm
    // ffmpeg -i input -passlogfile _1080web -vf scale="trunc(oh*a/2)*2:1080" -b:v 1800k -minrate 900k -maxrate 2610k -quality good -crf 31 -c:v libvpx-vp9 -c:a libopus -pass 1 -speed 4    1080.webm
    // ffmpeg -i input -passlogfile _1080web -vf scale="trunc(oh*a/2)*2:1080" -b:v 1800k -minrate 900k -maxrate 2610k -quality good -crf 31 -c:v libvpx-vp9 -c:a libopus -pass 2 -speed 4 -y 1080.webm

    static private List<Integer> SUPPORTED_SIZES = Arrays.asList(480, 720, 1080);
    static private String[] PRESET_0480 = {"-passlogfile", "log_pref", "-vf", "scale=\"trunc(oh*a/2)*2:480\"", "-b:v", "750k", "-minrate", "375k", "-maxrate", "1088k"};
    static private String[] PRESET_0720 = {"-passlogfile", "log_pref", "-vf", "scale=\"trunc(oh*a/2)*2:720\"", "-b:v", "1024k", "-minrate", "512k", "-maxrate", "1485k"};
    static private String[] PRESET_1080 = {"-passlogfile", "log_pref", "-vf", "scale=\"trunc(oh*a/2)*2:1080\"", "-b:v", "1800k", "-minrate", "900k", "-maxrate", "2610k"};

    Path transcode(MultimediaObject multimediaObject, File targetFolder, String type, int size) throws Exception {
        if (!SUPPORTED_SIZES.contains(size))
            throw new UnsupportedOperationException("Unsupported resolution: " + type);
        List<String> preset1p = new ArrayList<>(Arrays.asList(size == 480 ? PRESET_0480 : size == 720 ? PRESET_0720 : PRESET_1080));
        String outputFile = size + "." + type;
        String logFilePrefix = new File(targetFolder, "_" + size + type).getAbsolutePath();
        preset1p.set(preset1p.indexOf("log_pref"), logFilePrefix);
        switch (type) {
            case "mp4":
                preset1p.addAll(Arrays.asList("-c:v", "libx264", "-pass"));
                List<String> preset2p = new ArrayList<>(preset1p);
                preset1p.addAll(Arrays.asList("1", "-an", "-f", "mp4"));
                preset2p.addAll(Arrays.asList("2", "-c:a", "aac", "-b:a", "128k"));
                encode(multimediaObject, targetFolder, null, preset1p);
                encode(multimediaObject, targetFolder, outputFile, preset2p);
                break;
            case "webm":
                String crf_val = size == 480 ? "33" : size == 720 ? "32" : "31";
                preset1p.addAll(Arrays.asList("-quality", "good", "-crf", crf_val, "-c:v", "libvpx-vp9", "-c:a", "libopus", "-b:a", "128k", "-pass", "1", "-speed", "4"));
                encode(multimediaObject, targetFolder, outputFile, preset1p);
                preset1p.set(preset1p.indexOf("-pass") + 1, "2");
                encode(multimediaObject, targetFolder, outputFile, preset1p);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported format: " + type);
        }
        return targetFolder.toPath().resolve(outputFile);
    }
}
