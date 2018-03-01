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

import ai.apptuit.metrics.dropwizard.ApptuitReporter.ReportingMode;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides access to Configuration options
 *
 * @author Rajiv Shivane
 */
public class ConfigService {

  private static final Logger LOGGER = Logger.getLogger(ConfigService.class.getName());

  private static final String CONFIG_SYSTEM_PROPERTY = "jinsight.config";
  private static final String DEFAULT_CONFIG_FILE_NAME = "jinsight-config.properties";

  private static final String ACCESS_TOKEN_PROPERTY_NAME = "apptuit.access_token";
  private static final String API_ENDPOINT_PROPERTY_NAME = "apptuit.api_url";
  private static final String REPORTING_MODE_PROPERTY_NAME = "apptuit.reporting_mode";
  private static final String GLOBAL_TAGS_PROPERTY_NAME = "global_tags";
  private static final String HOST_TAG_NAME = "host";

  private static final File JINSIGHT_HOME = new File(System.getProperty("user.home"), ".jinsight");
  private static final File DEFAULT_CONFIG_FILE = new File(JINSIGHT_HOME, DEFAULT_CONFIG_FILE_NAME);
  private static final ReportingMode DEFAULT_REPORTING_MODE = ReportingMode.API_PUT;


  private static volatile ConfigService singleton = null;
  private final String apiToken;
  private final URL apiUrl;
  private final ReportingMode reportingMode;
  private final Map<String, String> loadedGlobalTags = new HashMap<>();
  private Map<String, String> globalTags = null;

  ConfigService(Properties config) throws ConfigurationException {
    this.apiToken = config.getProperty(ACCESS_TOKEN_PROPERTY_NAME);

    String configMode = config.getProperty(REPORTING_MODE_PROPERTY_NAME);
    ReportingMode mode = null;
    if (configMode != null) {
      try {
        mode = ReportingMode.valueOf(configMode.trim().toUpperCase());
      } catch (IllegalArgumentException e) {
        LOGGER.severe("Un-supported reporting mode [" + configMode + "]. "
            + "Using default reporting mode: [" + DEFAULT_REPORTING_MODE + "]");
        LOGGER.log(Level.FINE, e.toString(), e);
      }
    }
    reportingMode = (mode == null) ? DEFAULT_REPORTING_MODE : mode;

    if (apiToken == null) {
      if (reportingMode == ReportingMode.API_PUT) {
        throw new ConfigurationException(
            "Could not find the property [" + ACCESS_TOKEN_PROPERTY_NAME + "]");
      }
    }

    String configUrl = config.getProperty(API_ENDPOINT_PROPERTY_NAME);
    URL url = null;
    if (configUrl != null) {
      try {
        url = new URL(configUrl.trim());
      } catch (MalformedURLException e) {
        LOGGER.severe("Malformed API URL [" + configUrl + "]. Using default URL instead");
        LOGGER.log(Level.FINE, e.toString(), e);
        configUrl = null;
      }
    }
    this.apiUrl = url;

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

    try {
      singleton = new ConfigService(config);
    } catch (ConfigurationException e) {
      throw new ConfigurationException("Error loading configuration from the file ["
          + configFilePath + "]: " + e.getMessage(), e);
    }
  }

  private static Properties loadProperties(File configFilePath) throws IOException {
    Properties config = new Properties();
    try (InputStream inputStream = new BufferedInputStream(new FileInputStream(configFilePath))) {
      config.load(inputStream);
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
            loadedGlobalTags.put(tag, value);
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
    if (globalTags != null) {
      return globalTags;
    }

    globalTags = Collections.unmodifiableMap(_getGlobalTags());
    return globalTags;
  }

  private Map<String, String> _getGlobalTags() {
    if (getReportingMode() != ReportingMode.API_PUT) {
      return loadedGlobalTags;
    }

    String hostname = loadedGlobalTags.get(HOST_TAG_NAME);
    if (hostname != null && !"".equals(hostname.trim())) {
      return loadedGlobalTags;
    }

    try {
      hostname = InetAddress.getLocalHost().getCanonicalHostName();
    } catch (UnknownHostException e) {
      hostname = "?";
    }
    Map<String, String> retVal = new HashMap<>(loadedGlobalTags);
    retVal.put(HOST_TAG_NAME, hostname);
    return retVal;
  }

  URL getApiUrl() {
    return apiUrl;
  }

  ReportingMode getReportingMode() {
    return reportingMode;
  }
}
