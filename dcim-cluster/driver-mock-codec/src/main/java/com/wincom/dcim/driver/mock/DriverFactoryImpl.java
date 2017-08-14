package com.wincom.dcim.driver.mock;

import java.util.Map;

import com.wincom.dcim.driver.DriverCodecFactory;

import akka.actor.Props;
import scala.Option;

public class DriverFactoryImpl implements DriverCodecFactory {
	
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
