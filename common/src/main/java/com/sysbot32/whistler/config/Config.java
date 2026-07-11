package com.sysbot32.whistler.config;

public interface Config {
    String get(String key);

    String get(String key, String defaultValue);

    void set(String key, String value);

    void save();
}
