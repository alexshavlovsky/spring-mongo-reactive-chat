package com.ctzn.springmongoreactivechat.service;

import com.github.alexpumpkin.reactorlock.concurrency.LockCacheMono;
import com.github.alexpumpkin.reactorlock.concurrency.LockMono;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
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
import reactor.core.publisher.Signal;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Service
public class CacheThumbsServiceJpg240x240 implements ThumbsService {

    private Logger LOG = LoggerFactory.getLogger(CacheThumbsServiceJpg240x240.class);

    private AttachmentService attachmentService;

    public CacheThumbsServiceJpg240x240(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @Override
    public MediaType getMediaType() {
        return MediaType.IMAGE_JPEG;
    }

    private Cache<ThumbKey, Signal<byte[]>> graphs = Caffeine
            .newBuilder()
            .maximumSize(1_000)
            .expireAfterAccess(60, TimeUnit.MINUTES)
            .build();

    @Override
    public Mono<byte[]> getThumb(ThumbKey key) {
        return LockCacheMono
                .create(LockMono.key(key).build())
                .lookup(graphs.asMap())
                .onCacheMissResume(() -> Mono.defer(() -> createExpensiveGraph(key)));
        // see also https://en.wikipedia.org/wiki/Cache_stampede
        // see also https://stackoverflow.com/questions/58182516/cacheflux-how-to-synchronize-lookup-oncachemissresume
        // see also https://github.com/reactor/reactor-addons/issues/234
        // see also https://github.com/alex-pumpkin/reactor-lock
    }

    private Mono<byte[]> createExpensiveGraph(ThumbKey key) {
        return DataBufferUtils
                .join(attachmentService.getAttachmentById(key.fileId))
                .flatMap(dataBuffer -> {
                    try {
                        LOG.info("Generate a {} thumb of {}", key.thumbType, key.fileId);
                        return Mono.just(generateThumb(key.thumbType, dataBuffer));
                    } catch (IOException e) {
                        return Mono.error(e);
                    }
                });
    }

    private byte[] generateThumb(String thumbType, DataBuffer dataBuffer) throws IOException {
        ByteArrayOutputStream arrayBuffer = new ByteArrayOutputStream();
        Thumbnails.Builder builder = "pdf".equals(thumbType) ?
                Thumbnails.of(pdfAsImage(dataBuffer)) :
                Thumbnails.of(dataBuffer.asInputStream(true));
        builder.size(240, 240).outputFormat("jpg").toOutputStream(arrayBuffer);
        return arrayBuffer.toByteArray();
    }

    private BufferedImage pdfAsImage(DataBuffer dataBuffer) throws IOException {
        PDFFile pdf = new PDFFile(dataBuffer.asByteBuffer());
        PDFPage page = pdf.getPage(0);
        Rectangle rect = new Rectangle(0, 0, (int) page.getBBox().getWidth(), (int) page.getBBox().getHeight());
        BufferedImage bufferedImage = new BufferedImage(rect.width, rect.height, BufferedImage.TYPE_INT_RGB);
        Image image = page.getImage(rect.width, rect.height, rect, null, true, true);
        Graphics2D bufImageGraphics = bufferedImage.createGraphics();
        bufImageGraphics.drawImage(image, 0, 0, null);
        return bufferedImage;
    }
}
