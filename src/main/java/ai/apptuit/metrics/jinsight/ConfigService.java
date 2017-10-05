/*
 * Copyright 2017 Agilx, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.apptuit.metrics.jinsight;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Provides access to Configuration options
 *
 * @author Rajiv Shivane
 */
public class ConfigService {

  private static final String CONFIG_SYSTEM_PROPERTY = "jinsight.config";
  private static final String DEFAULT_CONFIG_FILE_NAME = "jinsight-config.properties";

  private static final String ACCESS_TOKEN_PROPERTY_NAME = "apptuit.access_token";
  private static final String API_ENDPOINT_PROPERTY_NAME = "apptuit.api_url";
  private static final String GLOBAL_TAGS_PROPERTY_NAME = "global_tags";

  private static final File JINSIGHT_HOME = new File(System.getProperty("user.home"), ".jinsight");
  private static final File DEFAULT_CONFIG_FILE = new File(JINSIGHT_HOME, DEFAULT_CONFIG_FILE_NAME);


  private static ConfigService singleton = null;
  private final String apiToken;
  private String apiUrl;
  private final Map<String, String> globalTags = new HashMap<>();

  ConfigService(Properties config) throws ConfigurationException {
    this.apiToken = config.getProperty(ACCESS_TOKEN_PROPERTY_NAME);
    this.apiUrl = config.getProperty(API_ENDPOINT_PROPERTY_NAME);

    loadGlobalTags(config);

  }

  public static ConfigService getInstance() {
    if (singleton == null) {
      synchronized (ConfigService.class) {
        if (singleton == null) {
          try {
            initialize();
          } catch (IOException | ConfigurationException e) {
            throw new IllegalStateException(e);
          }
        }
      }
    }
    return singleton;
  }

  static void initialize() throws IOException, ConfigurationException {

    if (singleton != null) {
      throw new IllegalStateException(
          ConfigService.class.getSimpleName() + " already initialized.");
    }

    Properties config;
    String configFilePath = System.getProperty(CONFIG_SYSTEM_PROPERTY);
    if (configFilePath != null && configFilePath.trim().length() > 0) {
      File configFile = new File(configFilePath);
      if (!configFile.exists() || !configFile.canRead()) {
        throw new FileNotFoundException("Could not find or read config file: ["
            + configFile.getAbsolutePath() + "]");
      }
      config = loadProperties(new File(configFilePath));
    } else if (DEFAULT_CONFIG_FILE.exists() && DEFAULT_CONFIG_FILE.canRead()) {
      configFilePath = DEFAULT_CONFIG_FILE.getAbsolutePath();
      config = loadProperties(DEFAULT_CONFIG_FILE);
    } else {
      throw new ConfigurationException("Could not find configuration file. "
          + "Set the path to configuration file using the system property \""
          + CONFIG_SYSTEM_PROPERTY + "\"");
    }
    String token = config.getProperty(ACCESS_TOKEN_PROPERTY_NAME);

    if (token == null) {
      throw new ConfigurationException("Could not find the property ["
          + ACCESS_TOKEN_PROPERTY_NAME + "] in the file ["
          + configFilePath + "]");
    }
    singleton = new ConfigService(config);
  }

  private static Properties loadProperties(File configFilePath) throws IOException {
    Properties config = new Properties();
    try (FileInputStream fileInputStream = new FileInputStream(configFilePath)) {
      config.load(new BufferedInputStream(fileInputStream));
    }
    return config;
  }

  private void loadGlobalTags(Properties config) throws ConfigurationException {
    String tagsString = config.getProperty(GLOBAL_TAGS_PROPERTY_NAME);
    if (tagsString != null) {
      String[] tvPairs = tagsString.split(",");
      for (String tvPair : tvPairs) {
        String[] tagAndValue = tvPair.split(":");
        if (tagAndValue.length == 2) {
          String tag = tagAndValue[0].trim();
          String value = tagAndValue[1].trim();
          if (tag.length() > 0 && value.length() > 0) {
            globalTags.put(tag, value);
            continue;
          }
        }
        throw new ConfigurationException("Error parsing " + GLOBAL_TAGS_PROPERTY_NAME
            + " property: [" + tvPair + "].\n"
            + "Expected format: " + GLOBAL_TAGS_PROPERTY_NAME
            + "=key1:value1,key2:value2,key3:value3");
      }
    }
  }

  String getApiToken() {
    return apiToken;
  }

  Map<String, String> getGlobalTags() {
    return Collections.unmodifiableMap(globalTags);
  }

  public String getApiUrl() {
    return apiUrl;
  }
}
