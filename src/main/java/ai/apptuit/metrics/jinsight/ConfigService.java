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

  private static final File DEFAULT_CONFIG_FILE = new File(System.getProperty("user.home"),
      DEFAULT_CONFIG_FILE_NAME);


  private static ConfigService singleton = null;
  private final String apiToken;

  private ConfigService(String apiToken) {
    this.apiToken = apiToken;
  }

  public static ConfigService getInstance() {
    if (singleton == null) {
      throw new IllegalStateException(ConfigService.class.getSimpleName() + " not initialized.");
    }
    return singleton;
  }

  static void initialize() throws IOException, IllegalArgumentException {

    Properties config = null;
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
      throw new IllegalArgumentException("Could not find configuration file. "
          + "Set the path to configuration file using the system property \""
          + CONFIG_SYSTEM_PROPERTY + "\"");
    }
    String token = config.getProperty(ACCESS_TOKEN_PROPERTY_NAME);

    if (token == null) {
      throw new IllegalArgumentException("Could not find the property ["
          + ACCESS_TOKEN_PROPERTY_NAME + "] in the file ["
          + configFilePath + "]");
    }
    singleton = new ConfigService(token);
  }

  private static Properties loadProperties(File configFilePath) throws IOException {
    Properties config = new Properties();
    try (FileInputStream fileInputStream = new FileInputStream(configFilePath)) {
      config.load(new BufferedInputStream(fileInputStream));
    }
    return config;
  }

  String getApiToken() {
    return apiToken;
  }
}
