package com.wincom.dcim.fsu;

import static java.lang.System.out;

import java.util.Map;
import java.util.TreeMap;

import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.Props;
import scala.Option;

public class FsuCodecRegistry {
	private Logger log = LoggerFactory.getLogger(this.getClass());
	private Map<String, FsuCodecFactory> factories;

	public FsuCodecRegistry() {
		this.factories = new TreeMap<>();
	}

	public Option<Props> create(String name, Map<String, String> params) {
		FsuCodecFactory factory = factories.get(name);
		if (factory != null) {
			return factory.create(params);
		} else {
			return Option.apply(null);
		}
	}

	public void initialize() {
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
					log.warn("Duplicate DriverCodecFactory modelName '{}': {} and {}", f.modelName(), c,
							factories.get(f.modelName()).getClass());
				} else {
					factories.put(f.modelName(), f);
				}
			}
		} catch (Exception ex) {
			log.error("DriverCodecFactory initializing failed: {}", ex);
		}
	}

	public static void main(String[] args) throws Exception {
		FsuCodecRegistry registry = new FsuCodecRegistry();
		registry.initialize();
		for (Map.Entry<String, FsuCodecFactory> e : registry.factories.entrySet()) {
			out.println(String.format("DriverCodecFactory(%s, %s)", e.getKey(), e.getValue()));
		}
	}
}
