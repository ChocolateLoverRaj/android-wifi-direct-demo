package com.example.wifidirectdemo;

import android.os.Build;

import androidx.annotation.RequiresApi;

// TODO: Put on maven or something
public abstract class MacAddress {
    public static MacAddress fromString(String addr) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                ? MacAddressBuiltin.fromString(addr)
                : MacAddressPolyfill.fromString(addr);
    }

    abstract public byte[] toByteArray();

    @RequiresApi(api = Build.VERSION_CODES.P)
    private static class MacAddressBuiltin extends MacAddress {
        private android.net.MacAddress macAddress;

        public static MacAddressBuiltin fromString(String addr) {
            MacAddressBuiltin macAddressBuiltin = new MacAddressBuiltin();
            macAddressBuiltin.macAddress = android.net.MacAddress.fromString(addr);
            return macAddressBuiltin;
        }

        @Override
        public byte[] toByteArray() {
            return macAddress.toByteArray();
        }
    }

    private static class MacAddressPolyfill extends MacAddress {
        private byte[] macAddress;

        public static MacAddressPolyfill fromString(String addr) {
            MacAddressPolyfill macAddressPolyfill = new MacAddressPolyfill();
            macAddressPolyfill.macAddress = new byte[6];
            String[] bytes = addr.split(":");
            int i = 0;
            for (String s : bytes)
                macAddressPolyfill.macAddress[i++] = (byte) ((Character.digit(s.charAt(0), 16) << 4)
                        + Character.digit(s.charAt(1), 16));
            return macAddressPolyfill;
        }

        @Override
        public byte[] toByteArray() {
            return macAddress.clone();
        }
    }
}
