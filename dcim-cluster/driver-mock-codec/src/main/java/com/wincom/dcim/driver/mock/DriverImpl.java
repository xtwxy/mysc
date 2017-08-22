package com.wincom.dcim.driver.mock;

import akka.actor.AbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.http.scaladsl.model.DateTime;
import com.wincom.dcim.domain.Driver.*;
import com.wincom.dcim.util.CollectionCoverter;
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
							new SignalValueVo(
									o.driverId(),
									o.key(),
									DateTime.apply(System.currentTimeMillis()),
									Double.valueOf(1997.71)
							),
							getSelf()
					);
				})
				.match(GetSignalValuesCmd.class, o -> {
					List<SignalValue> values = new ArrayList<>();
					for(String key : JavaConverters.asJavaIterable(o.keys())) {
						values.add(new SignalValue(
								key,
								DateTime.apply(System.currentTimeMillis()),
								Double.valueOf(Math.random())
							)
						);
					}
					getSender().tell(
							new SignalValuesVo(
									o.driverId(),
									JavaConverters.asScalaBuffer(values).toSeq()
							),
							getSelf()
					);
				})
				.match(SetSignalValueCmd.class, o -> {
					getSender().tell(
							new SetSignalValueRsp(
									o.driverId(),
									"OK"
							),
							getSelf()
					);
				})
				.match(SetSignalValuesCmd.class, o -> {
					Map<String, String> results = new HashMap<>();
					for(String key : JavaConverters.asJavaIterable(o.values().keys())) {
						results.put(key,"OK");
					}
					getSender().tell(
							new SetSignalValuesRsp(
									o.driverId(),
									CollectionCoverter.toImmutableMap(results)
							),
							getSelf()
					);
				})
				.match(SendBytesCmd.class, o -> {
					StringBuilder sb = new StringBuilder();
					sb.append("bytes: ");
					for(byte b : o.bytes()) {
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
