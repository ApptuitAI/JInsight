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
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides access to Configuration options.
 *
 * @author Rajiv Shivane
**/


public class ConfigService {

  static final String PROMETHEUS_EXPORTER_PORT = "prometheus_exporter_port";
  static final String PROMETHEUS_METRICS_PATH = "prometheus_exporter_endpoints";
  static final String REPORTING_MODE_PROPERTY_NAME = "apptuit.reporting_mode";
  static final String REPORTING_FREQ_PROPERTY_NAME = "reporting_frequency";
  private static final Logger LOGGER = Logger.getLogger(ConfigService.class.getName());
  private static final String CONFIG_SYSTEM_PROPERTY = "jinsight.config";
  private static final String DEFAULT_CONFIG_FILE_NAME = "jinsight-config.properties";
  private static final String ACCESS_TOKEN_PROPERTY_NAME = "apptuit.access_token";
  private static final String API_ENDPOINT_PROPERTY_NAME = "apptuit.api_url";
  private static final String GLOBAL_TAGS_PROPERTY_NAME = "global_tags";
  private static final String HOST_TAG_NAME = "host";

  private static final File JINSIGHT_HOME = new File(System.getProperty("user.home"), ".jinsight");
  private static final File UNIX_JINSIGHT_CONF_DIR = new File("/etc/jinsight/");
  private static final ReportingMode DEFAULT_REPORTING_MODE = ReportingMode.API_PUT;
  private static final String DEFAULT_REPORTING_FREQUENCY = "15s";
  private static final String DEFAULT_PROMETHEUS_EXPORTER_PORT = "9404";
  private static final String DEFAULT_PROMETHEUS_METRICS_PATH = "/metrics";

  private static volatile ConfigService singleton = null;
  private final String apiToken;
  private final URL apiUrl;
  private final ReportingMode reportingMode;
  private final long reportingFrequencyMillis;
  private final int prometheusPort;
  private final String prometheusMetricsPath;
  private final Map<String, String> loadedGlobalTags = new HashMap<>();
  private final String agentVersion;
  private Map<String, String> globalTags = null;
  private boolean isReportingModePrometheus;

  ConfigService(Properties config) throws ConfigurationException {
    this.apiToken = config.getProperty(ACCESS_TOKEN_PROPERTY_NAME);
    this.reportingMode = readReportingMode(config);
    this.reportingFrequencyMillis = readReportingFrequency(config);
    this.prometheusMetricsPath  = readPrometheusEndPoint(config);
    this.prometheusPort = readReportingPort(config);
    if (apiToken == null && reportingMode == ReportingMode.API_PUT) {
      throw new ConfigurationException(
              "Could not find the property [" + ACCESS_TOKEN_PROPERTY_NAME + "]");
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
    this.agentVersion = loadAgentVersion();

    loadGlobalTags(config);

  }

  /**
   *Returns the instance of the configService.
   */
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

    File configFile = getConfigFile();
    try {
      Properties config = loadProperties(configFile);
      singleton = new ConfigService(config);
    } catch (ConfigurationException e) {
      throw new ConfigurationException("Error loading configuration from the file ["
              + configFile + "]: " + e.getMessage(), e);
    }
  }

  private static File getConfigFile() throws FileNotFoundException, ConfigurationException {
    File configFile;
    String configFilePath = System.getProperty(CONFIG_SYSTEM_PROPERTY);
    if (configFilePath != null && configFilePath.trim().length() > 0) {
      configFile = new File(configFilePath);
      if (!configFile.exists() || !configFile.canRead()) {
        throw new FileNotFoundException("Could not find or read config file: ["
                + configFile.getAbsolutePath() + "]");
      }
    } else if (canLoadDefaultProperties(UNIX_JINSIGHT_CONF_DIR)) {
      configFile = new File(UNIX_JINSIGHT_CONF_DIR, DEFAULT_CONFIG_FILE_NAME);
    } else if (canLoadDefaultProperties(JINSIGHT_HOME)) {
      configFile = new File(JINSIGHT_HOME, DEFAULT_CONFIG_FILE_NAME);
    } else {
      throw new ConfigurationException("Could not find configuration file. "
              + "Set the path to configuration file using the system property \""
              + CONFIG_SYSTEM_PROPERTY + "\"");
    }
    return configFile;
  }

  private static boolean canLoadDefaultProperties(File folder) {
    File configFile = new File(folder, DEFAULT_CONFIG_FILE_NAME);
    return (configFile.exists() && configFile.canRead());
  }

  private static Properties loadProperties(File configFilePath) throws IOException {
    Properties config = new Properties();
    try (InputStream inputStream = new BufferedInputStream(new FileInputStream(configFilePath))) {
      config.load(inputStream);
    }
    return config;
  }

  private ReportingMode readReportingMode(Properties config) {
    String configMode = config.getProperty(REPORTING_MODE_PROPERTY_NAME);
    //As at present cant edit the AgentReporter
    //AFTER UPDATE comment out this "if" block and make "else" block as if block
    if (configMode != null && configMode.trim().toUpperCase().equals("PROMETHEUS")) {
      this.isReportingModePrometheus = true;
      return null;
    } else if (configMode != null && !configMode.equals("")) {
      try {
        return ReportingMode.valueOf(configMode.trim().toUpperCase());
      } catch (IllegalArgumentException e) {
        LOGGER.severe("Un-supported reporting mode [" + configMode + "]. "
                + "Using default reporting mode: [" + DEFAULT_REPORTING_MODE + "]");
        LOGGER.log(Level.FINE, e.toString(), e);
      }
    }
    return DEFAULT_REPORTING_MODE;
  }

  private String readPrometheusEndPoint(Properties config) {
    String configFreq = config.getProperty(PROMETHEUS_METRICS_PATH);
    if (configFreq != null) {
      return configFreq;
    }
    return DEFAULT_PROMETHEUS_METRICS_PATH;
  }

  private long readReportingFrequency(Properties config) {
    String configFreq = config.getProperty(REPORTING_FREQ_PROPERTY_NAME);
    if (configFreq != null) {
      try {
        return parseDuration(configFreq);
      } catch (DateTimeParseException | IllegalArgumentException e) {
        LOGGER.severe("Invalid reporting frequency [" + configFreq + "]. "
                + "Using default reporting frequency: [" + DEFAULT_REPORTING_FREQUENCY + "]");
        LOGGER.log(Level.FINE, e.toString(), e);
      }
    }
    return parseDuration(DEFAULT_REPORTING_FREQUENCY);
  }

  private int readReportingPort(Properties config) {
    String configPort = config.getProperty(PROMETHEUS_EXPORTER_PORT,
            DEFAULT_PROMETHEUS_EXPORTER_PORT);
    if (configPort != null) {
      try {
        return Integer.parseInt(configPort);
      } catch (NumberFormatException e) {
        LOGGER.severe("Invalid port [" + configPort + "]. "
                + "Using default port: [" + DEFAULT_PROMETHEUS_EXPORTER_PORT + "]");
        LOGGER.log(Level.FINE, e.toString(), e);
      }
    }
    return Integer.parseInt(DEFAULT_PROMETHEUS_EXPORTER_PORT);
  }

  private long parseDuration(String durationString) {
    long millis = Duration.parse("PT" + durationString.trim()).toMillis();
    if (millis < 0) {
      throw new IllegalArgumentException("Frequency cannot be negative");
    }
    return millis;
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
    globalTags = Collections.unmodifiableMap(createGlobalTagsMap());
    return globalTags;
  }

  boolean isReportingModePrometheus() {
    return this.isReportingModePrometheus;
  }

  private Map<String, String> createGlobalTagsMap() {
    Map<String, String> retVal = new HashMap<>(loadedGlobalTags);

    if (getReportingMode() == ReportingMode.API_PUT) {
      String hostname = retVal.get(HOST_TAG_NAME);
      if (hostname == null || "".equals(hostname.trim())) {
        try {
          hostname = InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException e) {
          hostname = "?";
        }
        retVal.put(HOST_TAG_NAME, hostname);
      }
    }
    return retVal;
  }

  URL getApiUrl() {
    return apiUrl;
  }

  ReportingMode getReportingMode() {
    return reportingMode;
  }

  long getReportingFrequency() {
    return reportingFrequencyMillis;
  }

  int getReportingPort() {
    return prometheusPort;
  }

  String getPrometheusExporterEndPoint() {
    return prometheusMetricsPath;
  }

  public String getAgentVersion() {
    return agentVersion;
  }

  private String loadAgentVersion() {
    Enumeration<URL> resources = null;
    try {
      resources = ConfigService.class.getClassLoader().getResources("META-INF/MANIFEST.MF");
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Error locating manifests.", e);
    }

    while (resources != null && resources.hasMoreElements()) {
      URL manifestUrl = resources.nextElement();
      try (InputStream resource = manifestUrl.openStream()) {
        Manifest manifest = new Manifest(resource);
        Attributes mainAttributes = manifest.getMainAttributes();
        if (mainAttributes != null) {
          String agentClass = mainAttributes.getValue("Agent-Class");
          if (Agent.class.getName().equals(agentClass)) {
            String agentVersion = mainAttributes.getValue("Agent-Version");
            if (agentVersion != null) {
              return agentVersion;
            }
            break;
          }
        }
      } catch (IOException e) {
        LOGGER.log(Level.SEVERE, "Error loading manifest from [" + manifestUrl + "]", e);
      }

    }
    return "?";
  }
}
