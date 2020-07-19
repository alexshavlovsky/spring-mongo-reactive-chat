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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@Document
@TypeAlias("compound-web-videos")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class CompoundWebVideo {

    @Id
    @JsonIgnore
    private String id;
    @JsonIgnore
    AttachmentModel attachment; // original video file
    String poster;              // '/path/to/poster.jpg'
    List<VideoSource> sources;  // video sources of various formats and sizes
    String previewThumbnails;   // '/path/to/thumbnails.vtt'

    public static CompoundWebVideo newInstance(AttachmentModel attachment) {
        CompoundWebVideo instance = new CompoundWebVideo();
        instance.setAttachment(attachment);
        instance.setSources(new ArrayList<>());
        return instance;
    }
}
