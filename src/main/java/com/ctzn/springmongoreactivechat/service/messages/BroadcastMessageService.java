package com.ctzn.springmongoreactivechat.service.messages;

import com.ctzn.springmongoreactivechat.domain.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BroadcastMessageService {
    Flux<Message> getTopic();

    Mono<Message> saveMessage(Message message);
}
