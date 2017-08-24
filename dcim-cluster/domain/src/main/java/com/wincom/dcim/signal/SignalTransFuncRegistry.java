package com.wincom.dcim.signal;

import akka.event.LoggingAdapter;
import com.wincom.dcim.driver.DriverCodecFactory;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import scala.Option;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Created by wangxy on 17-8-24.
 */
public class SignalTransFuncRegistry {
    LoggingAdapter log;
    private final Map<String, SignalTransFuncFactory> factories;

    public SignalTransFuncRegistry(LoggingAdapter log) {
        this.log = log;
        this.factories = new TreeMap<>();
    }

    public Set<String> names() {
        return factories.keySet();
    }

    public Set<String> paramNames(String name) {
        SignalTransFuncFactory factory = factories.get(name);
        if (factory != null) {
            return factory.paramNames();
        } else {
            return new HashSet<>();
        }
    }

    public Option<SignalTransFunc> create(String name, Map<String, String> params) {
        SignalTransFuncFactory factory = factories.get(name);
        if (factory != null) {
            return factory.create(params);
        } else {
            return Option.apply(null);
        }
    }

    public SignalTransFuncRegistry initialize() {
        try {
            FilterBuilder filter = new FilterBuilder()
                    .include("com\\.wincom.*");

            Reflections r = new Reflections(
                    new ConfigurationBuilder()
                            .filterInputsBy(filter)
                            .addScanners(new SubTypesScanner(false))
                            .setUrls(ClasspathHelper.forClassLoader()));

            for (Class<? extends SignalTransFuncFactory> c : r.getSubTypesOf(SignalTransFuncFactory.class)) {
                SignalTransFuncFactory f = c.newInstance();
                if (factories.containsKey(f.name())) {
                    log.warning("Duplicate SignalTransformerFactory name '{}': {} and {}", f.name(), c,
                            factories.get(f.name()).getClass());
                } else {
                    factories.put(f.name(), f);
                }
            }
        } catch (Exception ex) {
            log.error("SignalTransformerFactory initializing failed: {}", ex);
        }
        return this;
    }
}
