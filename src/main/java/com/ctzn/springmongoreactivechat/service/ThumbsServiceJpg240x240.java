package com.ctzn.springmongoreactivechat.service;

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

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

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
                .join(attachmentService.getAttachmentById(key.fileId))
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
        Thumbnails.Builder builder = "pdf".equals(thumbType) ?
                Thumbnails.of(pdfAsImage(dataBuffer)) :
                Thumbnails.of(dataBuffer.asInputStream(true));
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
}
