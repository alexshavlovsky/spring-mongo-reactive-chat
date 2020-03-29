package com.ctzn.springmongoreactivechat.domain;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.reactive.socket.WebSocketSession;

@Data
@RequiredArgsConstructor
public class ChatClient {
    @NonNull
    String sessionId;

    public static ChatClient fromSession(WebSocketSession session) {
        return new ChatClient(session.getId());
    }
}
