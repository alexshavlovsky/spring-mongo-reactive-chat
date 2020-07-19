package com.ctzn.springmongoreactivechat.service.videotranscoder;

import com.ctzn.springmongoreactivechat.domain.CompoundWebVideo;
import com.ctzn.springmongoreactivechat.domain.TranscodingJob;
import com.ctzn.springmongoreactivechat.domain.dto.AttachmentModel;
import com.ctzn.springmongoreactivechat.domain.dto.VideoSource;
import com.ctzn.springmongoreactivechat.repository.CompoundWebVideoRepository;
import com.ctzn.springmongoreactivechat.service.FileUtil;
import com.ctzn.springmongoreactivechat.service.attachments.AttachmentService;
import com.ctzn.springmongoreactivechat.service.ffmpeglocator.FfmpegLocatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import reactor.core.publisher.Flux;
import ws.schild.jave.MultimediaObject;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

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
    private CompoundWebVideoRepository compoundWebVideoRepository;

    public MongoVideoTranscoderService(ReactiveMongoOperations mongo, AttachmentService attachmentService, FfmpegLocatorService ffmpegLocatorService, TranscodingJobService transcodingJobService, CompoundWebVideoRepository compoundWebVideoRepository) {
        this.mongo = mongo;
        this.attachmentService = attachmentService;
        this.ffmpegLocatorService = ffmpegLocatorService;
        this.transcodingJobService = transcodingJobService;
        this.compoundWebVideoRepository = compoundWebVideoRepository;
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
//                        TranscodingJob.Preset.WEBM_480,
//                        TranscodingJob.Preset.WEBM_720
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
                    } catch (Exception e) {
                        return transcodingJobService.updateJobStatus(job, TranscodingJob.Status.ERROR, e.getMessage())
                                .doOnNext(j -> LOG.error("Error executing job: {}", j));
                    }
                })
                .block();
    }

    private void logInternal(TranscodingJob job, String message) {
        transcodingJobService.updateLog(job, message).block();
    }

    private void executeJob(TranscodingJob job) throws Exception {
        logInternal(job, "Initialisation");
        String fileId = job.getAttachment().getFileId();
        Path tempPath = FileUtil.getTempFolder().resolve(fileId);
        FileUtil.createFolder(tempPath);
        String sourceFileName = "source";
        Path sourceFilePath = tempPath.resolve(sourceFileName);
        if (Files.notExists(sourceFilePath)) {
            logInternal(job, "Copy the source file to temp folder");
            DataBufferUtils.write(attachmentService.getAttachmentById(fileId), sourceFilePath).thenReturn(true)
                    .blockOptional().orElseThrow(() -> new FileNotFoundException("Source file does not exist: " + fileId));
        }

        logInternal(job, "Create an executing process");
        MultimediaObject multimediaObject = new MultimediaObject(sourceFilePath.toFile());
        FfmpegExecutor executor = new FfmpegExecutor(ffmpegLocatorService.getInstance());
        Path result = executor.transcode(multimediaObject, tempPath.toFile(), job.getType(), job.getSize());

        logInternal(job, "Store a result using attachment service");
        String attachmentName = fileId + '_' + job.getSize() + '.' + job.getType();

        String attachmentId = attachmentService.store(DataBufferUtils.read(result, new DefaultDataBufferFactory(), 4096, StandardOpenOption.READ), attachmentName)
                .blockOptional().orElseThrow(() -> new FileNotFoundException("Error storing resulting file: " + attachmentName));

        logInternal(job, "Add a new video source");
        VideoSource videoSource = new VideoSource(attachmentId, "video/" + job.getType(), job.getSize());
        String compoundWebVideoId = job.getCompoundWebVideo().getId();
        CompoundWebVideo compoundWebVideo = compoundWebVideoRepository.findById(compoundWebVideoId)
                .blockOptional().orElseThrow(() -> new FileNotFoundException("CompoundWevVideo does not exist: " + compoundWebVideoId));
        compoundWebVideo.getSources().add(videoSource);
        compoundWebVideoRepository.save(compoundWebVideo).block();
        if (transcodingJobService.countPendingJobsByCompoundWebVideo_id(compoundWebVideoId) == 0) {
            logInternal(job, "Delete temp files");
            FileSystemUtils.deleteRecursively(tempPath);
        }
    }
}
