package com.ctzn.springmongoreactivechat.service.thumbs;

import com.ctzn.springmongoreactivechat.service.attachments.AttachmentService;
import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPage;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ws.schild.jave.FFMPEGLocator;
import ws.schild.jave.MultimediaObject;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

// TODO: This class is too large and needs to be refactored
@Service
@Qualifier("subject")
public class ThumbsServiceJpg240x240 implements ThumbsService {

    private Logger LOG = LoggerFactory.getLogger(ThumbsServiceJpg240x240.class);

    private AttachmentService attachmentService;

    public ThumbsServiceJpg240x240(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @Override
    public MediaType getMediaType() {
        return MediaType.IMAGE_JPEG;
    }

    @Override
    public Mono<byte[]> getThumb(ThumbKey key) {
        return DataBufferUtils
                .join(attachmentService.getAttachmentById(key.fileId)
                        .switchIfEmpty(Mono.error(() -> new Exception("File does not exist: " + key.fileId))))
                .flatMap(dataBuffer -> {
                    try {
                        LOG.info("Generate a {} thumb of {}", key.thumbType, key.fileId);
                        return Mono.just(generateThumb(key.thumbType, dataBuffer));
                    } catch (Exception e) {
                        return Mono.error(e);
                    }
                });
    }

    private byte[] generateThumb(String thumbType, DataBuffer dataBuffer) throws Exception {
        ByteArrayOutputStream arrayBuffer = new ByteArrayOutputStream();
        Thumbnails.Builder builder;
        switch (thumbType) {
            case "pdf":
                builder = Thumbnails.of(pdfAsImage(dataBuffer));
                break;
            case "video":
                builder = Thumbnails.of(videoAsImage(dataBuffer));
                break;
            default:
                builder = Thumbnails.of(dataBuffer.asInputStream(true));
        }
        builder.size(240, 240).outputFormat("JPEG").outputQuality(0.9).toOutputStream(arrayBuffer);
        return arrayBuffer.toByteArray();
    }

    private BufferedImage pdfAsImage(DataBuffer dataBuffer) throws Exception {
        PDFFile pdf = new PDFFile(dataBuffer.asByteBuffer());
        if (pdf.getNumPages() == 0) throw new Exception("PDF renderer: Can't parse any page");
        PDFPage page = pdf.getPage(0);
        Rectangle rect = new Rectangle(0, 0, (int) page.getBBox().getWidth(), (int) page.getBBox().getHeight());
        BufferedImage bufferedImage = new BufferedImage(rect.width, rect.height, BufferedImage.TYPE_INT_RGB);
        Image image = page.getImage(rect.width, rect.height, rect, null, true, true);
        Graphics2D bufImageGraphics = bufferedImage.createGraphics();
        bufImageGraphics.drawImage(image, 0, 0, null);
        return bufferedImage;
    }

    private final String TEMP_FOLDER_PATH = "app_temp_folder";
    private final Path tempPath = Paths.get(TEMP_FOLDER_PATH);
    private final FFMPEGLocator locator = new CustomFfmpegLocator();

    private InputStream videoAsImage(DataBuffer dataBuffer) throws Exception {
        if (!Files.exists(tempPath)) Files.createDirectories(tempPath);
        String randomFileName = UUID.randomUUID().toString();
        File sourceFile = tempPath.resolve(randomFileName).toFile();
        Path thumbPath = tempPath.resolve(randomFileName + ".jpg");
        try (FileChannel fc = new FileOutputStream(sourceFile).getChannel()) {
            fc.write(dataBuffer.asByteBuffer());
        }
        try {
            MultimediaObject multimediaObject = new MultimediaObject(sourceFile, locator);
            long duration = multimediaObject.getInfo().getDuration();
            ScreenExtractorTmp screenExtractor = new ScreenExtractorTmp(locator);
            screenExtractor.renderOneImage(multimediaObject, -1, -1, duration / 2, thumbPath.toFile(), 2, true);
            if (Files.exists(thumbPath))
                return new ByteArrayInputStream(Files.readAllBytes(thumbPath));
            else
                throw new RuntimeException("FFmpeg did not yield a screenshot image");
        } finally {
            if (sourceFile.exists()) sourceFile.delete();
            if (Files.exists(thumbPath)) Files.delete(thumbPath);
        }
    }
}
