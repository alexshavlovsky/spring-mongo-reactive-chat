package com.ctzn.springmongoreactivechat.service;

import com.ctzn.springmongoreactivechat.domain.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

// this publisher is neither persistent, nor replay
// it can be used for non-essential auxiliary messages such as a setTyping message, and an updateMe message
@Service
public class BroadcastEmitterService {

    private Logger LOG = LoggerFactory.getLogger(BroadcastEmitterService.class);

    private final Sinks.Many<Message> multicastSink = Sinks.many().multicast().onBackpressureBuffer(32, false);
    private final Flux<Message> multicastFlux = multicastSink.asFlux();

    public Flux<Message> getTopic() {
        return multicastFlux;
    }

    synchronized public void send(Message message) {
        Sinks.EmitResult result = multicastSink.tryEmitNext(message);
        if (result != Sinks.EmitResult.OK) LOG.error("Emmit result: {}", result);
    }
}
