package com.batch.process.dataservice;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class ContextMap {
    public final Map<String, Object> map = new ConcurrentHashMap<String, Object>();

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) map.get(key);
    }

    public void put(String key, Object value) {
        map.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T remove(String key) {
        return (T) map.remove(key);
    }

    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    public Set<String> keySet() {
        return map.keySet();
    }

    public Set<Entry<String, Object>> entrySet() {
        return map.entrySet();
    }

    public void forEach(BiConsumer<? super String, ? super Object> action) {
        map.forEach(action);
    }

}
