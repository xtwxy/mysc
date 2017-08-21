package com.wincom.dcim.driver.mock;

import akka.actor.Props;
import com.wincom.dcim.driver.DriverCodecFactory;
import scala.Option;

import java.util.Map;

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
