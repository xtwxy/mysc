package com.wincom.dcim.fsu;

import akka.actor.Props;
import akka.event.LoggingAdapter;
import com.wincom.dcim.message.common.ParamMeta;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import scala.Option;

import java.util.*;

public class FsuCodecRegistry {
	private final LoggingAdapter log;
	private final Map<String, FsuCodecFactory> factories;

	public FsuCodecRegistry(LoggingAdapter log) {
		this.log = log;
		this.factories = new TreeMap<>();
	}

	public Map<String, String> names() {
		Map<String, String> s = new HashMap<>();
		for(FsuCodecFactory f : factories.values()) {
			s.put(f.modelName(), f.displayName());
		}
		return s;
	}

	public Set<ParamMeta> paramOptions(String name) {
		FsuCodecFactory factory = factories.get(name);
		if (factory != null) {
			return factory.paramOptions();
		} else {
			return new HashSet<>();
		}
	}

	public Option<Props> create(String name, Map<String, String> params) {
		FsuCodecFactory factory = factories.get(name);
		if (factory != null) {
			return factory.create(params);
		} else {
			return Option.apply(null);
		}
	}

	public FsuCodecRegistry initialize() {
		try {
			FilterBuilder filter = new FilterBuilder()
					.include("com\\.wincom.*");
		
			Reflections r = new Reflections(
					new ConfigurationBuilder()
					.filterInputsBy(filter)
					.addScanners(new SubTypesScanner(false))
					.setUrls(ClasspathHelper.forClassLoader()));
			
			for (Class<? extends FsuCodecFactory> c : r.getSubTypesOf(FsuCodecFactory.class)) {
				FsuCodecFactory f = c.newInstance();
				if (factories.containsKey(f.modelName())) {
					log.warning("Duplicate DriverCodecFactory modelName '{}': {} and {}", f.modelName(), c,
							factories.get(f.modelName()).getClass());
				} else {
					factories.put(f.modelName(), f);
				}
			}
		} catch (Exception ex) {
			log.error("DriverCodecFactory initializing failed: {}", ex);
		}
		return this;
	}
}
