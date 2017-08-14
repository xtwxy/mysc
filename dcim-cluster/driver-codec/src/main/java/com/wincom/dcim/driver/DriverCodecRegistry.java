package com.wincom.dcim.driver;

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

import scala.Option;

public class DriverCodecRegistry {
	private Logger log = LoggerFactory.getLogger(this.getClass());
	private Map<String, DriverCodecFactory> factories;

	public DriverCodecRegistry() {
		this.factories = new TreeMap<>();
	}

	public Option<DriverCodec> create(String name, Map<String, String> params) {
		DriverCodecFactory factory = factories.get(name);
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
			
			for (Class<? extends DriverCodecFactory> c : r.getSubTypesOf(DriverCodecFactory.class)) {
				DriverCodecFactory f = c.newInstance();
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
		DriverCodecRegistry registry = new DriverCodecRegistry();
		registry.initialize();
		for (Map.Entry<String, DriverCodecFactory> e : registry.factories.entrySet()) {
			out.println(String.format("DriverCodecFactory(%s, %s)", e.getKey(), e.getValue()));
		}
	}
}
