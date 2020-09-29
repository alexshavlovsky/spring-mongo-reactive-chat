package com.ctzn.springmongoreactivechat.service.thumbs;

import com.ctzn.springmongoreactivechat.service.FileUtil;
import com.ctzn.springmongoreactivechat.service.ffmpeglocator.FfmpegLocatorService;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Component;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.ScreenExtractor;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Component
public class VideoThumbBuilder implements ThumbBuilderResolver {

    private FfmpegLocatorService ffmpegLocatorService;

    public VideoThumbBuilder(FfmpegLocatorService ffmpegLocatorService) {
        this.ffmpegLocatorService = ffmpegLocatorService;
    }

    @Override
    public Thumbnails.Builder resolve(String thumbType, DataBuffer dataBuffer) throws Exception {
        if (!"video".equals(thumbType)) return null;
        Path tempPath = FileUtil.getTempFolder();
        String randomFileName = UUID.randomUUID().toString();
        File sourceFile = tempPath.resolve(randomFileName).toFile();
        Path thumbPath = tempPath.resolve(randomFileName + ".jpg");
        try (FileChannel fc = new FileOutputStream(sourceFile).getChannel()) {
            fc.write(dataBuffer.asByteBuffer());
        }
        try {
            MultimediaObject multimediaObject = new MultimediaObject(sourceFile, ffmpegLocatorService.getInstance());
            long duration = multimediaObject.getInfo().getDuration();
            ScreenExtractor screenExtractor = new ScreenExtractor(ffmpegLocatorService.getInstance());
            screenExtractor.renderOneImage(multimediaObject, -1, -1, duration / 2, thumbPath.toFile(), 2);
            if (Files.exists(thumbPath))
                return Thumbnails.of(new ByteArrayInputStream(Files.readAllBytes(thumbPath)));
            else
                throw new RuntimeException("FFmpeg did not yield a screenshot image");
        } finally {
            if (sourceFile.exists()) sourceFile.delete();
            if (Files.exists(thumbPath)) Files.delete(thumbPath);
        }
    }
}
