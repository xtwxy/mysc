package com.wincom.dcim.hello;

import java.util.Map;

import com.wincom.dcim.driver.Driver;
import com.wincom.dcim.driver.DriverFactory;

import scala.Option;

public class HelloFactory implements DriverFactory {

	@Override
	public String name() {
		return "Hello";
	}

	@Override
	public Option<Driver> create(Map<String, String> params) {
		return Option.apply(null);
	}

}
