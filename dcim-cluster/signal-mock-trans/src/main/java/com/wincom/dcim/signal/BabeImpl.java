package com.wincom.dcim.signal;

import java.util.Map;

/**
 * Created by wangxy on 17-8-24.
 */
public class BabeImpl implements SignalTransFunc {
    private final Map<String, String> params;
    public BabeImpl(Map<String, String> params) {
        this.params = params;
    }

    @Override
    public Object transform(Object input) {
        if(input instanceof Boolean) {
            return !((Boolean)input);
        } else if(input instanceof Double) {
           return ((Double)input) * 10;
        }
        return input;
    }

    @Override
    public Object inverse(Object input) {
        if(input instanceof Boolean) {
            return !((Boolean)input);
        } else if(input instanceof Double) {
            return ((Double)input) / 10;
        }
        return input;
    }
}
