package com.ctzn.springmongoreactivechat.service.videotranscoder;

import com.ctzn.springmongoreactivechat.domain.CompoundWebVideo;
import com.ctzn.springmongoreactivechat.domain.dto.AttachmentModel;
import com.ctzn.springmongoreactivechat.service.attachments.AttachmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Profile("mongo-video-transcoder")
@EnableScheduling
// this service is assumed to persist intermediate data in mongo DB
// and to store encoded video files using AttachmentService
public class MongoVideoTranscoderService implements VideoTranscoderService {

    private Logger LOG = LoggerFactory.getLogger(MongoVideoTranscoderService.class);

    private ReactiveMongoOperations mongo;
    private AttachmentService attachmentService;

    public MongoVideoTranscoderService(ReactiveMongoOperations mongo, AttachmentService attachmentService) {
        this.mongo = mongo;
        this.attachmentService = attachmentService;
    }

    @Override
    public void addTask(AttachmentModel attachment) {
        CompoundWebVideo compoundWebVideo = CompoundWebVideo.newInstance(attachment);
        mongo.save(compoundWebVideo)
                .doOnNext(newCompoundWebVideo -> LOG.info("New task accepted: {}", compoundWebVideo))
                .doOnError(e -> LOG.error("Error creating new task: {}", e.toString()))
                .subscribe();
    }

    @Scheduled(fixedRate = 30 * 1000)
    public void work() {
        mongo.find(new BasicQuery("{$or: [{'status':'ENCODING'},{'status':'QUEUED'}], 'pendingJobs': {$exists: true, $ne: []}}"), CompoundWebVideo.class)
                .doOnNext(v -> LOG.info("Pending video: {}", v))
                .subscribe();
    }
}
