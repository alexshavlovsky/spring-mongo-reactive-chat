package com.ctzn.springmongoreactivechat.domain;

import com.ctzn.springmongoreactivechat.domain.dto.ChatSnapshot;
import com.ctzn.springmongoreactivechat.domain.dto.ChatSnapshotUpdate;
import com.ctzn.springmongoreactivechat.domain.dto.VideoSourceUpdate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class DomainMapper {

    private static final ObjectMapper mapper = new ObjectMapper();

    public String toJson(Object object) {
        try {
            return mapper.writeValueAsString(object);
        } catch (Exception e) {
            e.printStackTrace();
            return "{}";
        }
    }

    public <T> T fromJson(String json, Class<T> clazz) throws Exception {
        return mapper.readValue(json, clazz);
    }

    public Message toMessage(ChatSnapshot snapshot) {
        return Message.newSnapshot(toJson(snapshot));
    }

    public Message toMessage(ChatSnapshotUpdate snapshotUpdate) {
        return Message.newSnapshotUpdate(toJson(snapshotUpdate));
    }

    public Message toMessage(VideoSourceUpdate videoSourceUpdate) {
        return Message.newVideoSourceUpdate(toJson(videoSourceUpdate));
    }
}
