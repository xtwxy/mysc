package com.wincom.dcim.driver.mock;

import akka.actor.AbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.google.protobuf.timestamp.Timestamp;
import com.wincom.dcim.message.common.ResponseType;
import com.wincom.dcim.message.driver.*;
import com.wincom.dcim.message.signal.*;
import com.wincom.dcim.util.CollectionCoverter;
import scala.Option;
import scala.collection.JavaConverters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
                .match(GetSignalValueCmd.class, o -> {
                    log.info("sender(): {}, msg: {}", sender(), o);
                    getSender().tell(
                            new DriverSignalSnapshotVo(
                                    o.id(),
                                    o.key(),
                                    Timestamp.defaultInstance(),
                                    SignalValueVo.apply(SignalType.AI$.MODULE$, Option.empty(), Option.apply(1.414), Option.empty())
                            ),
                            getSelf()
                    );
                })
                .match(GetSignalValuesCmd.class, o -> {
                    List<DriverSignalSnapshotVo> values = new ArrayList<>();
                    for (String key : JavaConverters.asJavaIterable(o.keys())) {
                        values.add(
                                new DriverSignalSnapshotVo(
                                        o.id(),
                                        key,
                                        Timestamp.defaultInstance(),
                                        SignalValueVo.apply(SignalType.AI$.MODULE$, Option.empty(), Option.apply(1.414), Option.empty())
                                )
                        );
                    }
                    getSender().tell(
                            new DriverSignalSnapshotsVo(
                                    JavaConverters.asScalaBuffer(values).toSeq()
                            ),
                            getSelf()
                    );
                })
                .match(SetSignalValueCmd.class, o -> {
                    getSender().tell(
                            com.wincom.dcim.message.signal.SetValueRsp.apply(
                                    ResponseType.SUCCESS$.MODULE$,
                                    Option.apply("OK")
                            ),
                            getSelf()
                    );
                })
                .match(SetSignalValuesCmd.class, o -> {
                    Map<String, com.wincom.dcim.message.signal.SetValueRsp> results = new HashMap<>();
                    for (String key : JavaConverters.asJavaIterable(o.values().keys())) {
                        results.put(key, SetValueRsp.apply(ResponseType.SUCCESS$.MODULE$, Option.apply("OK")));
                    }
                    getSender().tell(
                            new SetSignalValuesRsp(
                                    CollectionCoverter.toImmutableMap(results)
                            ),
                            getSelf()
                    );
                })
                .match(SendBytesCmd.class, o -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append("bytes: ");
                    for (byte b : o.bytes()) {
                        sb.append(String.format("%02x ", 0xff & b));
                    }
                    log.info(sb.toString());
                })
                .matchAny(o -> {
                    log.info("received: {}, type: {}", o, o.getClass().getName());
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
