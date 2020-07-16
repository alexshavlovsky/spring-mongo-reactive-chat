package com.ctzn.springmongoreactivechat.domain;

import com.ctzn.springmongoreactivechat.domain.dto.AttachmentModel;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Document
@TypeAlias("transcoding-jobs")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class TranscodingJob {
    public enum Status {PENDING, EXECUTING, DONE, ERROR}

    public enum Preset {MP4_480, WEBM_480, MP4_720, WEBM_720, MP4_1080, WEBM_1080}

    @Id
    private String id;
    final private int priority;
    final private AttachmentModel attachment;
    final private String type;
    final private int size;
    final private CompoundWebVideo compoundWebVideo;
    final private Date createdOn;
    private Status status;
    private Date lastModified;
    private String lastMessage;
    private List<String> log;

    public void appendToLog(String message) {
        lastModified = new Date();
        lastMessage = message;
        log.add(String.format("%tc %s : %s", lastModified, status, lastMessage));
    }

    public void setStatusAndLog(Status status, String message) {
        this.status = status;
        appendToLog(message);
    }

    public TranscodingJob(String type, int size, int priority, AttachmentModel attachment, CompoundWebVideo compoundWebVideo, Date createdOn) {
        this.type = type;
        this.size = size;
        this.priority = priority;
        this.attachment = attachment;
        this.compoundWebVideo = compoundWebVideo;
        this.createdOn = createdOn;
        this.log = new ArrayList<>();
        setStatusAndLog(Status.PENDING, "Accepted");
    }

    public static TranscodingJob newInstance(Preset preset, AttachmentModel attachment, CompoundWebVideo compoundWebVideo) {
        Date createdOn = new Date();
        switch (preset) {
            case MP4_480:
                return new TranscodingJob("mp4", 480, 0, attachment, compoundWebVideo, createdOn);
            case WEBM_480:
                return new TranscodingJob("webm", 480, 1, attachment, compoundWebVideo, createdOn);
            case MP4_720:
                return new TranscodingJob("mp4", 720, 2, attachment, compoundWebVideo, createdOn);
            case WEBM_720:
                return new TranscodingJob("webm", 720, 3, attachment, compoundWebVideo, createdOn);
            case MP4_1080:
                return new TranscodingJob("mp4", 1080, 4, attachment, compoundWebVideo, createdOn);
            case WEBM_1080:
                return new TranscodingJob("webm", 1080, 5, attachment, compoundWebVideo, createdOn);
        }
        throw new RuntimeException("Unsupported preset: " + preset);
    }
}
