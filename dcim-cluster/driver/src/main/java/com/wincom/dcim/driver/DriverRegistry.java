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

public class DriverRegistry {
	private Logger log = LoggerFactory.getLogger(this.getClass());
	private Map<String, DriverFactory> factories;

	public DriverRegistry() {
		this.factories = new TreeMap<>();
	}

	Option<Driver> create(String name, Map<String, String> params) {
		DriverFactory factory = factories.get(name);
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
			
			for (Class<? extends DriverFactory> c : r.getSubTypesOf(DriverFactory.class)) {
				DriverFactory f = c.newInstance();
				if (factories.containsKey(f.name())) {
					log.warn("Duplicate DriverFactory name '{}': {} and {}", f.name(), c,
							factories.get(f.name()).getClass());
				} else {
					factories.put(f.name(), f);
				}
			}
		} catch (Exception ex) {
			log.error("DriverFactory initializing failed: {}", ex);
		}
	}

	public static void main(String[] args) throws Exception {
		DriverRegistry registry = new DriverRegistry();
		registry.initialize();
		for (Map.Entry<String, DriverFactory> e : registry.factories.entrySet()) {
			out.println(String.format("DriverFactory(%s, %s)", e.getKey(), e.getValue()));
		}
	}
}
