package com.wincom.dcim.fsu.mock;

import java.util.Map;

import com.wincom.dcim.fsu.FsuCodecFactory;

import akka.actor.Props;
import scala.Option;

public class DriverFactoryImpl implements FsuCodecFactory {
	
	@Override
	public String modelName() {
		return "driver-mock1";
	}

	@Override
	public Option<Props> create(Map<String, String> params) {
		Props p = Props.create(DriverImpl.class, params);
		return Option.apply(p);
	}
}
