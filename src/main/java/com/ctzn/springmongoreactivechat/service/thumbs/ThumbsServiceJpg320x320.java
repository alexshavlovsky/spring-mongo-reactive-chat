package com.ctzn.springmongoreactivechat.service.thumbs;

import com.ctzn.springmongoreactivechat.service.attachments.AttachmentService;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
@Qualifier("subject")
public class ThumbsServiceJpg320x320 implements ThumbsService {

    private Logger LOG = LoggerFactory.getLogger(ThumbsServiceJpg320x320.class);

    private AttachmentService attachmentService;
    private List<ThumbBuilderResolver> thumbBuilders;

    public ThumbsServiceJpg320x320(AttachmentService attachmentService, List<ThumbBuilderResolver> thumbBuilders) {
        this.attachmentService = attachmentService;
        this.thumbBuilders = thumbBuilders;
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
        // thumb builders from context (thumbType = PDF, video, ect.)
        Thumbnails.Builder builder = resolveThumbBuilder(thumbType, dataBuffer);
        // default thumb builder for an image attachment
        if (builder == null) builder = Thumbnails.of(dataBuffer.asInputStream(true));
        return renderThumb(builder);
    }

    private Thumbnails.Builder resolveThumbBuilder(String thumbType, DataBuffer dataBuffer) throws Exception {
        for (ThumbBuilderResolver thumbBuilder : thumbBuilders) {
            Thumbnails.Builder builder = thumbBuilder.resolve(thumbType, dataBuffer);
            if (builder != null) return builder;
        }
        return null;
    }

    private byte[] renderThumb(Thumbnails.Builder builder) throws IOException {
        ByteArrayOutputStream arrayBuffer = new ByteArrayOutputStream();
        builder.size(320, 320).outputFormat("JPEG").outputQuality(0.9).toOutputStream(arrayBuffer);
        return arrayBuffer.toByteArray();
    }
}
