package com.wincom.dcim.driver;

import java.util.Map;

import scala.Option;

public interface DriverFactory {
	String name();
	Option<Driver> create(Map<String, String> params);
}
