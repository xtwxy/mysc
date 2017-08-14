package com.wincom.dcim.driver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by wangxy on 17-8-14.
 */
public abstract class AbstractDriverCodec implements DriverCodec, DriverCodecHandler {
    private Logger log = LoggerFactory.getLogger(this.getClass());
    protected Sender sender;
    protected Sender datalink;

    @Override
    public void received(Sender s, Command m) {
        sender = s;
        m.execute(this);
    }

    @Override
    public void datalink(Sender s) {
        this.datalink = s;
    }

    @Override
    public void handle(Object anyRef) {
        log.info("default handler: message ignored: {}", anyRef);
    }
}
