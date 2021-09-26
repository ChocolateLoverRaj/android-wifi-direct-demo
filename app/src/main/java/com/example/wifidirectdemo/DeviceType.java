package com.example.wifidirectdemo;

import java.util.EnumMap;

public enum DeviceType {
    PRINTER,
    TV,
    MOBILE_DEVICE,
    UNKNOWN;

    public static DefaultedEnumMap<DeviceType, String> descriptionsMap = new DefaultedEnumMap<>(DeviceType.class, "unknown device");

    static {
        descriptionsMap.put(PRINTER, "printer");
        descriptionsMap.put(TV, "TV");
        descriptionsMap.put(MOBILE_DEVICE, "mobile device");
    }

    public static DefaultedHashMap<String, DeviceType> codesMap = new DefaultedHashMap<>(UNKNOWN);

    static {
        codesMap.put("3", PRINTER);
        codesMap.put("7", TV);
        codesMap.put("10", MOBILE_DEVICE);
    }

    public static DefaultedEnumMap<DeviceType, Integer> iconsMap = new DefaultedEnumMap<>(DeviceType.class, R.drawable.ic_baseline_device_unknown_24);

    static {
        iconsMap.put(PRINTER, R.drawable.ic_baseline_print_24);
        iconsMap.put(TV, R.drawable.ic_baseline_tv_24);
        iconsMap.put(MOBILE_DEVICE, R.drawable.ic_baseline_smartphone_24);
    }
}
