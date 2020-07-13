package com.ctzn.springmongoreactivechat.domain.dto;

import lombok.Value;

@Value
// the ultimate destination of this DTO is to be included as one of the sources in the html5 video tag on the frontend side
public class VideoSource {
    String src;   // '/path/to/movie.mp4' | '/path/to/movie.webm'
    String type;  // 'video/mp4'          | 'video/webm'
    int size;     // 720                  | 1080
}
