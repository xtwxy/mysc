package com.wincom.dcim.fsu;

import java.util.Map;
import java.util.Set;

import akka.actor.Props;
import com.wincom.dcim.message.common.ParamMeta;
import scala.Option;

public interface FsuCodecFactory {
	String modelName();
	String displayName();
	Set<ParamMeta> paramOptions();
	Option<Props> create(Map<String, String> params);
}
