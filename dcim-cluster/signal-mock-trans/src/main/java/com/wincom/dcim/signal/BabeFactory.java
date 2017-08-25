package com.wincom.dcim.signal;

import scala.Option;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by wangxy on 17-8-24.
 */
public class BabeFactory implements UnaryFunctionFactory {
    @Override
    public String name() {
        return "babe-signal";
    }

    @Override
    public Set<String> paramNames() {
        Set<String> s = new HashSet<>();
        s.add("a");
        s.add("b");
        return s;
    }

    @Override
    public Option<UnaryFunction> create(Map<String, String> params) {
        return Option.apply(new BabeImpl(params));
    }
}
