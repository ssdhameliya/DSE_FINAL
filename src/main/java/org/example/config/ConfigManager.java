package org.example.config;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class ConfigManager {

    private static final String CONFIG_FOLDER =
            System.getProperty("user.home") + File.separator + ".javaapp";

    private static final String CONFIG_FILE =
            CONFIG_FOLDER + File.separator + "config.properties";

    private static final Properties properties = new Properties();

    private ConfigManager() {
    }

    public static void load() {

        try {

            Path folder = Path.of(CONFIG_FOLDER);

            if (!Files.exists(folder)) {
                Files.createDirectories(folder);
            }

            File file = new File(CONFIG_FILE);

            if (file.exists()) {

                try (FileInputStream fis = new FileInputStream(file)) {
                    properties.load(fis);
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void save() {

        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {

            properties.store(fos, "JavaApp ERP Configuration");

        } catch (IOException e) {

            e.printStackTrace();

        }

    }

    public static String get(String key, String defaultValue) {

        return properties.getProperty(key, defaultValue);

    }

    public static void set(String key, String value) {

        properties.setProperty(key, value);
        save();

    }

}