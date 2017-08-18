package com.wincom.dcim.driver.mock;

import akka.actor.AbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.util.Map;

public class DriverImpl extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final Map<String, String> params;

    public DriverImpl(Map<String, String> params) {
        this.params = params;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .matchAny(o -> {
                    log.info("received: {}", o);
                    log.info("params: {}", params);
                })
                .build();
    }

    @Override
    public void preStart() {
        log.info("started: {}", getSelf());
    }
    @Override
    public void postStop() {
        log.info("stopped: {}", getSelf());
    }
}
