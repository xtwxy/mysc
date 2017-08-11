package com.wincom.dcim.test;

import java.util.Map;

import com.wincom.dcim.driver.Driver;
import com.wincom.dcim.driver.DriverFactory;

import scala.Option;

public class TestFactory implements DriverFactory {

	@Override
	public String name() {
		return "Test";
	}

	@Override
	public Option<Driver> create(Map<String, String> params) {
		return null;
	}

}
