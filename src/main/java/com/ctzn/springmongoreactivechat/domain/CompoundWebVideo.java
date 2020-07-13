package com.ctzn.springmongoreactivechat.domain;

//player.source ={
//        type:'video',
//        title:'Example title',
//        sources:[
//        {
//        src:'/path/to/movie.mp4',
//        type:'video/mp4',
//        size:720,
//        },
//        {
//        src:'/path/to/movie.webm',
//        type:'video/webm',
//        size:1080,
//        },
//        ],
//        poster:'/path/to/poster.jpg',
//        previewThumbnails:{
//        src:'/path/to/thumbnails.vtt',
//        }
//        }

import com.ctzn.springmongoreactivechat.domain.dto.AttachmentModel;
import com.ctzn.springmongoreactivechat.domain.dto.VideoSource;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Data
@Document
@TypeAlias("compound-web-videos")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class CompoundWebVideo {
    AttachmentModel attachment; // original video file
    String poster;              // '/path/to/poster.jpg'
    List<VideoSource> sources;  // video sources of various formats and sizes
    String previewThumbnails;   // '/path/to/thumbnails.vtt'

    public enum TranscodingStatus {QUEUED, ENCODING, OK, ERROR}

    public enum TranscodingJob {POSTER, THUMBNAILS, MP4_480, WEBM_480, MP4_720, WEBM_720, MP4_1080, WEBM_1080}

    TranscodingStatus status;
    String statusMessage;
    Date modified;
    List<String> log;
    List<TranscodingJob> pendingJobs;

    public void appendLog(TranscodingStatus newStatus, String newMessage) {
        setStatus(newStatus);
        setStatusMessage(newMessage);
        setModified(new Date());
        log.add(String.format("%tc %s : %s", modified, status, statusMessage));
    }

    public static CompoundWebVideo newInstance(AttachmentModel attachment) {
        CompoundWebVideo instance = new CompoundWebVideo();
        instance.setAttachment(attachment);
        instance.log = new ArrayList<>();
        instance.appendLog(TranscodingStatus.QUEUED, "Accepted");
        instance.setPendingJobs(Arrays.asList(TranscodingJob.MP4_480));
        instance.setSources(new ArrayList<>());
        return instance;
    }
}
