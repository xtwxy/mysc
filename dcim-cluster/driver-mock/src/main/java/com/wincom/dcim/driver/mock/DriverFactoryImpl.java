package com.wincom.dcim.driver.mock;

import java.util.Map;

import com.wincom.dcim.driver.Driver;
import com.wincom.dcim.driver.DriverFactory;

import scala.Option;

public class DriverFactoryImpl implements DriverFactory {
	
	@Override
	public String name() {
		return "driver-mock1";
	}

	@Override
	public Option<Driver> create(Map<String, String> params) {
		return Option.apply(new DriverImpl(params));
	}
}
