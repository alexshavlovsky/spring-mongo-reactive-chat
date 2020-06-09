package com.ctzn.springmongoreactivechat.service;

import com.ctzn.springmongoreactivechat.domain.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

// this publisher is neither persistent, nor replay
// it can be used for non-essential auxiliary messages such as a setTyping message
@Service
public class DirectBroadcastService {

    final private EmitterProcessor<Message> processor = EmitterProcessor.create(false);
    final private FluxSink<Message> sink = processor.sink(FluxSink.OverflowStrategy.BUFFER);

    public Flux<Message> getTopic() {
        return processor;
    }

    public void send(Message message) {
        sink.next(message);
    }
}
