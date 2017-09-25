package com.wincom.dcim.signal;

import akka.event.LoggingAdapter;
import com.wincom.dcim.message.common.ParamMeta;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import scala.Option;

import java.util.*;

/**
 * Created by wangxy on 17-8-24.
 */
public class FunctionRegistry {
    LoggingAdapter log;
    private final Map<String, UnaryFunctionFactory> unaryFactories;
    private final Map<String, BinaryFunctionFactory> binaryFactories;

    public FunctionRegistry(LoggingAdapter log) {
        this.log = log;
        this.unaryFactories = new TreeMap<>();
        this.binaryFactories = new TreeMap<>();
    }

    public Map<String, String> names() {
        Map<String, String> s = new HashMap<>();
        for(FunctionFactory f : unaryFactories.values()) {
            s.put(f.name(), f.displayName());
        }
        for(FunctionFactory f : binaryFactories.values()) {
            s.put(f.name(), f.displayName());
        }
        return s;
    }

    public Set<ParamMeta> paramOptions(String name) {
        FunctionFactory factory = unaryFactories.get(name);
        if (factory != null) {
            return factory.paramOptions();
        }
        factory = binaryFactories.get(name);
        if (factory != null) {
            return factory.paramOptions();
        } else {
            return new HashSet<>();
        }
    }

    public Option<UnaryFunction> createUnary(String name, Map<String, String> params) {
        UnaryFunctionFactory factory = unaryFactories.get(name);
        if (factory != null) {
            return factory.create(params);
        } else {
            return Option.apply(null);
        }
    }

    public Option<BinaryFunction> createBinary(String name, Map<String, String> params) {
        BinaryFunctionFactory factory = binaryFactories.get(name);
        if (factory != null) {
            return factory.create(params);
        } else {
            return Option.apply(null);
        }
    }

    public FunctionRegistry initialize() {
        try {
            FilterBuilder filter = new FilterBuilder()
                    .include("com\\.wincom.*");

            Reflections r = new Reflections(
                    new ConfigurationBuilder()
                            .filterInputsBy(filter)
                            .addScanners(new SubTypesScanner(false))
                            .setUrls(ClasspathHelper.forClassLoader()));

            for (Class<? extends UnaryFunctionFactory> c : r.getSubTypesOf(UnaryFunctionFactory.class)) {
                UnaryFunctionFactory f = c.newInstance();
                if (unaryFactories.containsKey(f.name())) {
                    log.warning("Duplicate SignalTransformerFactory name '{}': {} and {}", f.name(), c,
                            unaryFactories.get(f.name()).getClass());
                } else {
                    unaryFactories.put(f.name(), f);
                }
            }
            for (Class<? extends BinaryFunctionFactory> c : r.getSubTypesOf(BinaryFunctionFactory.class)) {
                BinaryFunctionFactory f = c.newInstance();
                if (binaryFactories.containsKey(f.name())) {
                    log.warning("Duplicate SignalTransformerFactory name '{}': {} and {}", f.name(), c,
                            binaryFactories.get(f.name()).getClass());
                } else {
                    binaryFactories.put(f.name(), f);
                }
            }
        } catch (Exception ex) {
            log.error("SignalTransformerFactory initializing failed: {}", ex);
        }
        return this;
    }
}
