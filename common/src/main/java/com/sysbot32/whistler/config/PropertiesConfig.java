package com.sysbot32.whistler.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

public class PropertiesConfig implements Config {
    private final Path path;
    private final Properties properties = new Properties();

    public PropertiesConfig(final Path path) {
        this.path = path;
        this.load();
    }

    private void load() {
        if (!Files.exists(this.path)) {
            return;
        }
        try (final InputStream in = Files.newInputStream(this.path)) {
            this.properties.load(in);
        } catch (final IOException e) {
            throw new RuntimeException("Failed to load config: " + this.path, e);
        }
    }

    @Override
    public String get(final String key) {
        return this.properties.getProperty(key);
    }

    @Override
    public String get(final String key, final String defaultValue) {
        return this.properties.getProperty(key, defaultValue);
    }

    @Override
    public void set(final String key, final String value) {
        if (Objects.isNull(value)) {
            this.properties.remove(key);
        } else {
            this.properties.setProperty(key, value);
        }
    }

    @Override
    public void save() {
        try {
            final Path parent = this.path.getParent();
            if (Objects.nonNull(parent)) {
                Files.createDirectories(parent);
            }
            try (final OutputStream out = Files.newOutputStream(this.path)) {
                this.properties.store(out, "Whistler settings");
            }
        } catch (final IOException e) {
            throw new RuntimeException("Failed to save config: " + this.path, e);
        }
    }
}
