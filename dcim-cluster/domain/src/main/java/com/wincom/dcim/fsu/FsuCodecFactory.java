package com.wincom.dcim.fsu;

import java.util.Map;
import java.util.Set;

import akka.actor.Props;
import scala.Option;

public interface FsuCodecFactory {
	String modelName();
	Set<String> paramNames();
	Option<Props> create(Map<String, String> params);
}
