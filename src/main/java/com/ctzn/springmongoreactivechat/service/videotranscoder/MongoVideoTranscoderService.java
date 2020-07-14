package com.ctzn.springmongoreactivechat.service.videotranscoder;

import com.ctzn.springmongoreactivechat.domain.CompoundWebVideo;
import com.ctzn.springmongoreactivechat.domain.TranscodingJob;
import com.ctzn.springmongoreactivechat.domain.dto.AttachmentModel;
import com.ctzn.springmongoreactivechat.service.FileUtil;
import com.ctzn.springmongoreactivechat.service.attachments.AttachmentService;
import com.ctzn.springmongoreactivechat.service.ffmpeglocator.FfmpegLocatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@Service
@Profile("mongo-video-transcoder")
@EnableScheduling
// this service is assumed to persist intermediate data in mongo DB
// and to store encoded video files using AttachmentService
public class MongoVideoTranscoderService implements VideoTranscoderService {

    private Logger LOG = LoggerFactory.getLogger(MongoVideoTranscoderService.class);

    private ReactiveMongoOperations mongo;
    private AttachmentService attachmentService;
    private FfmpegLocatorService ffmpegLocatorService;

    public MongoVideoTranscoderService(ReactiveMongoOperations mongo, AttachmentService attachmentService, FfmpegLocatorService ffmpegLocatorService) {
        this.mongo = mongo;
        this.attachmentService = attachmentService;
        this.ffmpegLocatorService = ffmpegLocatorService;
    }

    @Override
    public void addTask(AttachmentModel attachment) {
        CompoundWebVideo compoundWebVideoInstance = CompoundWebVideo.newInstance(attachment);
        mongo.save(compoundWebVideoInstance)
                .doOnNext(compoundWebVideo -> LOG.info("New video accepted: {}", compoundWebVideo))
                .doOnError(e -> LOG.error("Error accepting new video: {}", e.toString()))
                .flatMapMany(compoundWebVideo -> Flux.just(
                        TranscodingJob.Preset.MP4_480,
                        TranscodingJob.Preset.MP4_720
                ).map(preset -> TranscodingJob.newInstance(preset, attachment, compoundWebVideo)))
                .flatMap(transcodingJob -> mongo.save(transcodingJob))
                .doOnNext(transcodingJob -> LOG.info("New transcoding job created: {}", transcodingJob))
                .subscribe();
    }


    @Scheduled(fixedRate = 10 * 1000)
    public void work() {
        mongo.find(new BasicQuery("{'status': 'PENDING'}"), TranscodingJob.class)
                .sort(Comparator.comparingInt(TranscodingJob::getPriority).thenComparing(TranscodingJob::getCreatedOn))
                .next()
                .flatMap(job -> {
                    try {
                        executeJob(job);
                        System.out.println("Job is done: " + job.getId());
                        return mongo.updateFirst(query(where("_id").is(job.getId())), Update.update("status", TranscodingJob.Status.DONE), TranscodingJob.class).then();
                    } catch (IOException e) {
                        return Mono.error(e);
                    }
                })
                .subscribe();
    }

    private void executeJob(TranscodingJob job) throws IOException {
        Path tempPath = FileUtil.getTempFolder();
        FfmpegExecutor executor = new FfmpegExecutor(ffmpegLocatorService.getInstance());
//        TODO: implement this dummy method
    }
}
