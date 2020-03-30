package com.ctzn.springmongoreactivechat.service;

import com.ctzn.springmongoreactivechat.domain.Message;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ReplayProcessor;

@Service
@Profile("replay-service")
public class ReplayBroadcastMessageServiceAdapter implements BroadcastMessageService {

    private ReplayProcessor<Message> chatMessageHistory;

    public ReplayBroadcastMessageServiceAdapter() {
        chatMessageHistory = ReplayProcessor.create(50);
        chatMessageHistory.onNext(Message.newInfo("Service started"));
    }

    @Override
    public Flux<Message> getTopic() {
        return chatMessageHistory;
    }

    @Override
    public Mono<Message> saveMessage(Message message) {
        chatMessageHistory.onNext(message);
        return Mono.just(message);
    }
}
