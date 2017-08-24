package com.wincom.dcim.fsu.mock;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.wincom.dcim.fsu.FsuCodecFactory;

import akka.actor.Props;
import scala.Option;

public class DriverFactoryImpl implements FsuCodecFactory {
	
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
