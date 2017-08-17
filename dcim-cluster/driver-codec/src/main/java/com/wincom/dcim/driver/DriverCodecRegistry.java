package com.wincom.dcim.driver;

import akka.actor.Props;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Option;

import java.util.Map;
import java.util.TreeMap;

import static java.lang.System.out;

public class DriverCodecRegistry {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final Map<String, DriverCodecFactory> factories;

    public DriverCodecRegistry() {
        this.factories = new TreeMap<>();
    }

    public Option<Props> create(String name, Map<String, String> params) {
        DriverCodecFactory factory = factories.get(name);
        if (factory != null) {
            return factory.create(params);
        } else {
            return Option.apply(null);
        }
    }

    public DriverCodecRegistry initialize() {
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
        return this;
    }

    public static void main(String[] args) throws Exception {
        DriverCodecRegistry registry = new DriverCodecRegistry();
        registry.initialize();
        registry.factories.forEach((k, v) -> out.println(String.format("DriverCodecFactory(%s, %s)", k, v)));
    }
}
