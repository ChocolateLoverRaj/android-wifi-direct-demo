package com.example.wifidirectdemo;

import androidx.annotation.Nullable;

import java.util.HashMap;

// TODO: Move this to separate installable dependency
public class DefaultedHashMap<K, V> extends HashMap<K, V> {
    private final V defaultValue;

    public DefaultedHashMap(V defaultValue) {
        super();
        this.defaultValue = defaultValue;
    }

    @Override
    public V get(@Nullable Object key) {
        return containsKey(key) ? super.get(key) : defaultValue;
    }
}
