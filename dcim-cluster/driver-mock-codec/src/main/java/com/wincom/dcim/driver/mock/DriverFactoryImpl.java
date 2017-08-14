package com.wincom.dcim.driver.mock;

import java.util.Map;

import com.wincom.dcim.driver.DriverCodec;
import com.wincom.dcim.driver.DriverCodecFactory;

import scala.Option;

public class DriverFactoryImpl implements DriverCodecFactory {
	
	@Override
	public String modelName() {
		return "driver-mock1";
	}

	@Override
	public Option<DriverCodec> create(Map<String, String> params) {
		return Option.apply(new DriverImpl(params));
	}
}
