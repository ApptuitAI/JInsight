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

import ai.apptuit.metrics.client.Sanitizer;
import ai.apptuit.metrics.dropwizard.ApptuitReporter.ReportingMode;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides access to Configuration options.
 *
 * @author Rajiv Shivane
 */
public class ConfigService {

  private static final String CONFIG_SYSTEM_PROPERTY = "jinsight.config";
  private static final String DEFAULT_CONFIG_FILE_NAME = "jinsight-config.properties";

  public static final String REPORTING_FREQ_PROPERTY_NAME = "reporting_frequency";
  private static final String GLOBAL_TAGS_PROPERTY_NAME = "global_tags";
  public static final String REPORTER_PROPERTY_NAME = "reporter";

  public static final String PROMETHEUS_EXPORTER_PORT = "prometheus.exporter_port";
  public static final String PROMETHEUS_METRICS_PATH = "prometheus.exporter_endpoint";

  public static final String REPORTING_MODE_PROPERTY_NAME = "apptuit.reporting_mode";
  private static final String ACCESS_TOKEN_PROPERTY_NAME = "apptuit.access_token";
  private static final String API_ENDPOINT_PROPERTY_NAME = "apptuit.api_url";

  private static final String HOST_TAG_NAME = "host";
  private static final String UUID_TEMPLATE_VARIABLE = "${UUID}";
  private static final String PID_TEMPLATE_VARIABLE = "${PID}";

  private static final File JINSIGHT_HOME = new File(System.getProperty("user.home"), ".jinsight");
  private static final File UNIX_JINSIGHT_CONF_DIR = new File("/etc/jinsight/");
  private static final ReportingMode DEFAULT_REPORTING_MODE = ReportingMode.API_PUT;
  public static final ReporterType DEFAULT_REPORTER_TYPE = ReporterType.PROMETHEUS;
  private static final String DEFAULT_REPORTING_FREQUENCY = "15s";
  private static final String DEFAULT_PROMETHEUS_EXPORTER_PORT = "9404";
  private static final String DEFAULT_PROMETHEUS_METRICS_PATH = "/metrics";

  private static final Logger LOGGER = Logger.getLogger(ConfigService.class.getName());

  private static volatile ConfigService singleton = null;
  private final String apiToken;
  private final URL apiUrl;
  private final ReporterType reporterType;
  private final ReportingMode reportingMode;
  private final long reportingFrequencyMillis;
  private final int prometheusPort;
  private final String prometheusMetricsPath;
  private final Map<String, String> loadedGlobalTags = new HashMap<>();
  private final String agentVersion;
  private Map<String, String> globalTags = null;
  private final Sanitizer sanitizer;

  public enum ReporterType {
    PROMETHEUS, APPTUIT
  }

  ConfigService(Properties config) throws ConfigurationException {
    this.apiToken = config.getProperty(ACCESS_TOKEN_PROPERTY_NAME);
    this.reporterType = readReporter(config);
    this.reportingMode = readReportingMode(config);
    this.reportingFrequencyMillis = readReportingFrequency(config);
    this.prometheusMetricsPath = readPrometheusMetricsPath(config);
    this.sanitizer = readSanitizer(config);
    this.prometheusPort = readPrometheusPort(config);
    if (this.reporterType == ReporterType.APPTUIT && apiToken == null && reportingMode == ReportingMode.API_PUT) {
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
    // override with system properties, if present
    config.putAll(loadSystemProperties());
    return config;
  }

  private static Map<String, Object> loadSystemProperties() {
    Map<String, Object> systemProperties = new HashMap<>();
    String reporter = getProperty(REPORTER_PROPERTY_NAME);
    if(reporter != null && !reporter.equals("")){
      systemProperties.put(REPORTER_PROPERTY_NAME, reporter);
    }

    String accessToken = getProperty(ACCESS_TOKEN_PROPERTY_NAME);
    if(accessToken != null && !accessToken.equals("")){
      systemProperties.put(ACCESS_TOKEN_PROPERTY_NAME, accessToken);
    }

    String apiEndpoint = getProperty(API_ENDPOINT_PROPERTY_NAME);
    if(apiEndpoint != null && !apiEndpoint.equals("")){
      systemProperties.put(API_ENDPOINT_PROPERTY_NAME, apiEndpoint);
    }

    String globalTags = getProperty(GLOBAL_TAGS_PROPERTY_NAME);
    if(globalTags != null && !globalTags.equals("")){
      systemProperties.put(GLOBAL_TAGS_PROPERTY_NAME, globalTags);
    }

    return systemProperties;
  }

  private static String getProperty(String propertyName) {
    String propertyValue = System.getenv(propertyName);
    if(propertyValue == null || propertyValue.equals("")){
      propertyValue = System.getProperty(propertyName);
    }
    return propertyValue;
  }

  private Sanitizer readSanitizer(Properties config) {
    String configSanitizer = config.getProperty("apptuit.sanitizer");
    if (configSanitizer != null && !configSanitizer.equals("")) {
      try {
        if (configSanitizer.trim().equalsIgnoreCase("PROMETHEUS_SANITIZER")) {
          return Sanitizer.PROMETHEUS_SANITIZER;
        } else if (configSanitizer.trim().equalsIgnoreCase("APPTUIT_SANITIZER")) {
          return Sanitizer.APPTUIT_SANITIZER;
        } else if (configSanitizer.trim().equalsIgnoreCase("NO_OP_SANITIZER")) {
          return Sanitizer.NO_OP_SANITIZER;
        } else {
          throw new IllegalArgumentException();
        }
      } catch (IllegalArgumentException e) {
        LOGGER.severe("Un-supported sanitization type [" + configSanitizer + "]. "
                + "Using default sanitization type: [" + Sanitizer.DEFAULT_SANITIZER + "]");
        LOGGER.log(Level.FINE, e.toString(), e);
      }
    }
    return Sanitizer.DEFAULT_SANITIZER;
  }

  private ReporterType readReporter(Properties config) {
    String configReporter = config.getProperty(REPORTER_PROPERTY_NAME);
    if (configReporter != null && !configReporter.equals("")) {
      try {
        return ReporterType.valueOf(configReporter.trim().toUpperCase());
      } catch (IllegalArgumentException e) {
        LOGGER.severe("Un-supported reporting type [" + configReporter + "]. "
                + "Using default reporting type: [" + DEFAULT_REPORTER_TYPE + "]");
        LOGGER.log(Level.FINE, e.toString(), e);
      }
    }
    return DEFAULT_REPORTER_TYPE;
  }

  private ReportingMode readReportingMode(Properties config) {
    String configMode = config.getProperty(REPORTING_MODE_PROPERTY_NAME);
    if (configMode != null && !configMode.equals("")) {
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

  private String readPrometheusMetricsPath(Properties config) {
    String configPath = config.getProperty(PROMETHEUS_METRICS_PATH);
    if (configPath != null && !configPath.equals("")) {
      return configPath;
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

  private int readPrometheusPort(Properties config) {
    String configPort = config.getProperty(PROMETHEUS_EXPORTER_PORT,
            DEFAULT_PROMETHEUS_EXPORTER_PORT);
    if (configPort != null && !configPort.equals("")) {
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
          if (value.equalsIgnoreCase(UUID_TEMPLATE_VARIABLE)) {
            value = UUID.randomUUID().toString();
          } else if (value.equalsIgnoreCase(PID_TEMPLATE_VARIABLE)) {
            value = getThisJVMProcessID() + "";
          }
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

  static int getThisJVMProcessID() throws ConfigurationException {
    try {
      java.lang.management.RuntimeMXBean runtime =
              java.lang.management.ManagementFactory.getRuntimeMXBean();
      java.lang.reflect.Field jvm = runtime.getClass().getDeclaredField("jvm");
      jvm.setAccessible(true);
      sun.management.VMManagement mgmt =
              (sun.management.VMManagement) jvm.get(runtime);
      java.lang.reflect.Method pidMethod =
              mgmt.getClass().getDeclaredMethod("getProcessId");
      pidMethod.setAccessible(true);

      return (Integer) pidMethod.invoke(mgmt);
    } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
      throw new ConfigurationException("Error fetching " + PID_TEMPLATE_VARIABLE + " of JVM", e);
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

  private Map<String, String> createGlobalTagsMap() {
    Map<String, String> retVal = new HashMap<>(loadedGlobalTags);

    if (getReporterType() == ReporterType.APPTUIT && getReportingMode() == ReportingMode.API_PUT) {
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

  public ReporterType getReporterType() {
    return reporterType;
  }

  public ReportingMode getReportingMode() {
    return reportingMode;
  }

  long getReportingFrequency() {
    return reportingFrequencyMillis;
  }

  public int getPrometheusPort() {
    return prometheusPort;
  }

  public Sanitizer getSanitizer() {
    return sanitizer;
  }

  public String getPrometheusMetricsPath() {
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
