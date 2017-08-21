package com.wincom.dcim.driver.hello;

import java.util.Map;

import com.wincom.dcim.driver.DriverCodecFactory;

import akka.actor.Props;
import com.wincom.dcim.driver.mock.DriverImpl;
import scala.Option;

public class HelloFactory implements DriverCodecFactory {

	@Override
	public String modelName() {
		return "Hello";
	}

	@Override
	public Option<Props> create(Map<String, String> params) {
		Props p = Props.create(DriverImpl.class, params);
		return Option.apply(p);
	}

}
