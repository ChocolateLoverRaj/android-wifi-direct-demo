package com.example.wifidirectdemo;

import androidx.annotation.NonNull;

import java.util.EnumMap;

// TODO: Move this to separate installable dependency
public class DefaultedEnumMap<K extends Enum<K>, V> extends EnumMap<K, V> {
    private final V defaultValue;

    public DefaultedEnumMap(Class<K> keyType, V defaultValue) {
        super(keyType);
        this.defaultValue = defaultValue;
    }

    @NonNull
    @Override
    public V get(Object key) {
        final V existingValue = super.get(key);
        return existingValue != null ? existingValue : defaultValue;
    }
}
