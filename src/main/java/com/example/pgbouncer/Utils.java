package com.example.pgbouncer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;

public interface Utils {
  static Properties loadConfig(final String configFile) throws IOException {
    Path path = Paths.get(configFile);
    if (!Files.exists(path)) {
      throw new IOException(configFile + " not found.");
    }
    final Properties cfg = new Properties();
    try (InputStream inputStream = Files.newInputStream(path)) {
      cfg.load(inputStream);
    }
    return cfg;
  }

  static HashMap<String, String> propertiesToMap(String configFile) throws IOException {
    HashMap<String, String> sqlQueries = new HashMap<>();
    Properties cfg = loadConfig(configFile);
    Enumeration<String> enums = (Enumeration<String>) cfg.propertyNames();
    while (enums.hasMoreElements()) {
      String key = enums.nextElement();
      sqlQueries.put(key, cfg.getProperty(key));

    }
    return sqlQueries;

  }

  static HashMap<String, String> propertiesToMap(Properties cfg) throws IOException {
    HashMap<String, String> sqlQueries = new HashMap<>();
    Enumeration<String> enums = (Enumeration<String>) cfg.propertyNames();
    while (enums.hasMoreElements()) {
      String key = enums.nextElement();
      sqlQueries.put(key, cfg.getProperty(key));

    }
    return sqlQueries;

  }
}
