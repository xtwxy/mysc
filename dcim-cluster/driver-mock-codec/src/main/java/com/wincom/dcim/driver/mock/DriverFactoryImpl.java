package com.wincom.dcim.driver.mock;

import akka.actor.Props;
import com.wincom.dcim.driver.DriverCodecFactory;
import scala.Option;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DriverFactoryImpl implements DriverCodecFactory {
	
	@Override
	public String modelName() {
		return "driver-mock1";
	}

	@Override
	public Set<String> paramNames() {
		Set<String> s = new HashSet<>();
		s.add("a");
		s.add("b");
		return s;
	}

	@Override
	public Option<Props> create(Map<String, String> params) {
		Props p = Props.create(DriverImpl.class, params);
		return Option.apply(p);
	}
}
