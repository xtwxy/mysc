package com.wincom.dcim.driver;

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

public class DriverCodecRegistry {
    LoggingAdapter log;
    private final Map<String, DriverCodecFactory> factories;

    public DriverCodecRegistry(LoggingAdapter log) {
        this.log = log;
        this.factories = new TreeMap<>();
    }

    public Map<String, String> names() {
        Map<String, String> s = new HashMap<>();
        for(DriverCodecFactory f : factories.values()) {
            s.put(f.modelName(), f.displayName());
        }
        return s;
    }

    public Set<ParamMeta> paramOptions(String name) {
        DriverCodecFactory factory = factories.get(name);
        if (factory != null) {
            return factory.paramOptions();
        } else {
            return new HashSet<>();
        }
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
