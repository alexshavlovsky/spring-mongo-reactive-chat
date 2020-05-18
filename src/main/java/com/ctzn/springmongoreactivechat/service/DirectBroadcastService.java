package com.ctzn.springmongoreactivechat.service;

import com.ctzn.springmongoreactivechat.domain.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Flux;

// this publisher is neither persistent, nor replay, nor durable
// it can be used for non-essential auxiliary messages such as a setTyping message
@Service
public class DirectBroadcastService {

    private DirectProcessor<Message> sink;

    public DirectBroadcastService() {
        sink = DirectProcessor.create();
    }

    public Flux<Message> getTopic() {
        return sink;
    }

    public void send(Message message) {
        sink.onNext(message);
    }
}
