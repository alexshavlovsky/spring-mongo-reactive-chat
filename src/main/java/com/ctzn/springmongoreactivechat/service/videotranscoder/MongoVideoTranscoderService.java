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
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.file.Path;

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
    private TranscodingJobService transcodingJobService;

    public MongoVideoTranscoderService(ReactiveMongoOperations mongo, AttachmentService attachmentService, FfmpegLocatorService ffmpegLocatorService, TranscodingJobService transcodingJobService) {
        this.mongo = mongo;
        this.attachmentService = attachmentService;
        this.ffmpegLocatorService = ffmpegLocatorService;
        this.transcodingJobService = transcodingJobService;
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
                .flatMap(transcodingJob -> transcodingJobService.save(transcodingJob))
                .doOnNext(transcodingJob -> LOG.info("New transcoding job created: {}", transcodingJob))
                .subscribe();
    }


    @Scheduled(fixedRate = 10 * 1000)
    public void work() {
        transcodingJobService.getNextPendingJob()
                .flatMap(job -> transcodingJobService.updateJobStatus(job, TranscodingJob.Status.EXECUTING, "Start executing")
                        .doOnNext(j -> LOG.info("Start executing job: {}", j))
                )
                .flatMap(job -> {
                    try {
                        executeJob(job);
                        return transcodingJobService.updateJobStatus(job, TranscodingJob.Status.DONE, "Finished")
                                .doOnNext(j -> LOG.info("Job is finished: {}", j));
                    } catch (IOException e) {
                        return transcodingJobService.updateJobStatus(job, TranscodingJob.Status.ERROR, e.getMessage())
                                .doOnNext(j -> LOG.error("Error executing job: {}", j));
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
