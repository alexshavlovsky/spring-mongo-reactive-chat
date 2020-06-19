package com.ctzn.springmongoreactivechat.service;

import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPage;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
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
    public Mono<byte[]> getThumb(String fileId, String thumbType) {
        return DataBufferUtils
                .join(attachmentService.getAttachmentById(fileId))
                .flatMap(dataBuffer -> {
                    try {
                        return Mono.just(generateThumb(thumbType, dataBuffer));
                    } catch (IOException e) {
                        return Mono.error(e);
                    }
                });
    }

    private byte[] generateThumb(String thumbType, DataBuffer dataBuffer) throws IOException {
        System.out.println("Expensive");
        ByteArrayOutputStream arrayBuffer = new ByteArrayOutputStream();
        if ("pdf".equals(thumbType)) {
            BufferedImage page = pdfAsImage(dataBuffer);
            if (page != null) Thumbnails.of(page)
                    .size(240, 240).outputFormat("jpg").toOutputStream(arrayBuffer);
        } else {
            Thumbnails.of(dataBuffer.asInputStream(true))
                    .size(240, 240).outputFormat("jpg").toOutputStream(arrayBuffer);
        }
        return arrayBuffer.toByteArray();
    }

    private BufferedImage pdfAsImage(DataBuffer dataBuffer) {
        try {
            PDFFile pdf = new PDFFile(dataBuffer.asByteBuffer());
            PDFPage page = pdf.getPage(0);
            Rectangle rect = new Rectangle(0, 0, (int) page.getBBox().getWidth(), (int) page.getBBox().getHeight());
            BufferedImage bufferedImage = new BufferedImage(rect.width, rect.height, BufferedImage.TYPE_INT_RGB);
            Image image = page.getImage(rect.width, rect.height, rect, null, true, true);
            Graphics2D bufImageGraphics = bufferedImage.createGraphics();
            bufImageGraphics.drawImage(image, 0, 0, null);
            return bufferedImage;
        } catch (Exception e) {
            LOG.error(e.getMessage());
            return null;
        }
    }
}
