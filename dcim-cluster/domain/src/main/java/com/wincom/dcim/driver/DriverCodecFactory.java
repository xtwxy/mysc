package com.wincom.dcim.driver;

import java.util.Map;
import java.util.Set;

import com.wincom.dcim.message.common.ParamMeta;
import scala.Option;
import akka.actor.Props;

public interface DriverCodecFactory {
	String modelName();
	Set<ParamMeta> paramOptions();
	Option<Props> create(Map<String, String> params);
}
