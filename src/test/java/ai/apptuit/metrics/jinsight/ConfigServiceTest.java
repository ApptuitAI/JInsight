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
import ai.apptuit.metrics.jinsight.ConfigService.ReporterType;
import org.junit.Test;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import static ai.apptuit.metrics.jinsight.ConfigService.REPORTER_PROPERTY_NAME;
import static ai.apptuit.metrics.jinsight.ConfigService.REPORTING_FREQ_PROPERTY_NAME;
import static ai.apptuit.metrics.jinsight.ConfigService.REPORTING_MODE_PROPERTY_NAME;
import static ai.apptuit.metrics.jinsight.ConfigService.getThisJVMProcessID;
import static ai.apptuit.metrics.jinsight.ConfigService.initialize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Rajiv Shivane
 */

public class ConfigServiceTest {

  @Test
  public void testGetToken() throws Exception {
    Properties p = getDefaultConfigProperties();
    ConfigService configService = new ConfigService(p);
    assertEquals(p.getProperty("apptuit.access_token"), configService.getApiToken());
  }

  @Test(expected = ConfigurationException.class)
  public void testPutReporterNeedsToken() throws Exception {
    Properties p = new Properties();
    p.setProperty("reporter", ConfigService.ReporterType.APPTUIT.toString());
    ConfigService configService = new ConfigService(p);
  }

  @Test
  public void testGetSanitizerDefault() throws Exception {
    Properties p = getDefaultConfigProperties();
    ConfigService configService = new ConfigService(p);
    assertEquals(Sanitizer.DEFAULT_SANITIZER, configService.getSanitizer());
  }

  @Test
  public void testGetSanitizerJunk() throws Exception {
    Properties p = getDefaultConfigProperties();
    p.setProperty("apptuit.sanitizer", "Junk");
    ConfigService configService = new ConfigService(p);
    assertEquals(Sanitizer.DEFAULT_SANITIZER, configService.getSanitizer());
  }

  @Test
  public void testGetSanitizerPrometheusSanitizer() throws Exception {
    Properties p = getDefaultConfigProperties();
    p.setProperty("apptuit.sanitizer", "PROMETHEUS_SANITIZER");
    ConfigService configService = new ConfigService(p);
    assertEquals(Sanitizer.PROMETHEUS_SANITIZER, configService.getSanitizer());
  }

  @Test
  public void testGetSanitizerApptuitSanitizer() throws Exception {
    Properties p = getDefaultConfigProperties();
    p.setProperty("apptuit.sanitizer", "APPTUIT_SANITIZER");
    ConfigService configService = new ConfigService(p);
    assertEquals(Sanitizer.APPTUIT_SANITIZER, configService.getSanitizer());
  }

  @Test
  public void testGetSanitizerNoOpSanitizer() throws Exception {
    Properties p = getDefaultConfigProperties();
    p.setProperty("apptuit.sanitizer", "NO_OP_SANITIZER");
    ConfigService configService = new ConfigService(p);
    assertEquals(Sanitizer.NO_OP_SANITIZER, configService.getSanitizer());
  }

  @Test
  public void testGetReportingModeDefault() throws Exception {
    Properties p = getDefaultConfigProperties();
    ConfigService configService = new ConfigService(p);
    assertEquals(ReportingMode.API_PUT, configService.getReportingMode());
  }

  @Test
  public void testGetReportingModeJunk() throws Exception {
    Properties p = getDefaultConfigProperties();
    p.setProperty(REPORTING_MODE_PROPERTY_NAME, "junk");
    ConfigService configService = new ConfigService(p);
    assertEquals(ReportingMode.API_PUT, configService.getReportingMode());
  }

  @Test
  public void testGetReportingModeNoOp() throws Exception {
    Properties p = new Properties();
    p.setProperty(REPORTING_MODE_PROPERTY_NAME, "NO_OP");
    ConfigService configService = new ConfigService(p);
    assertEquals(ReportingMode.NO_OP, configService.getReportingMode());
  }

  @Test
  public void testGetReportingModeNoOpCaseInsensitive() throws Exception {
    Properties p = new Properties();
    p.setProperty(REPORTING_MODE_PROPERTY_NAME, "No_Op");
    ConfigService configService = new ConfigService(p);
    assertEquals(ReportingMode.NO_OP, configService.getReportingMode());
  }

  @Test
  public void testDefaultReportingFreq() throws Exception {
    Properties p = getDefaultConfigProperties();
    ConfigService configService = new ConfigService(p);
    assertEquals(15000L, configService.getReportingFrequency());
  }

  @Test
  public void testDefaultReportingFreqOnError() throws Exception {
    Properties p = getDefaultConfigProperties();
    p.setProperty(REPORTING_FREQ_PROPERTY_NAME, "BLEEP");
    ConfigService configService = new ConfigService(p);
    assertEquals(15000L, configService.getReportingFrequency());
  }

  @Test
  public void testDefaultReportingFreqSeconds() throws Exception {
    Properties p = getDefaultConfigProperties();
    p.setProperty(REPORTING_FREQ_PROPERTY_NAME, "13S");
    ConfigService configService = new ConfigService(p);
    assertEquals(13000L, configService.getReportingFrequency());
  }

  @Test
  public void testReportingFreqSecondsLowercase() throws Exception {
    Properties p = getDefaultConfigProperties();
    p.setProperty(REPORTING_FREQ_PROPERTY_NAME, "13s");
    ConfigService configService = new ConfigService(p);
    assertEquals(13000L, configService.getReportingFrequency());
  }

  @Test
  public void testReportingFreqPartialSeconds() throws Exception {
    Properties p = getDefaultConfigProperties();
    p.setProperty(REPORTING_FREQ_PROPERTY_NAME, "13.7s");
    ConfigService configService = new ConfigService(p);
    assertEquals(13700L, configService.getReportingFrequency());
  }

  @Test
  public void testDefaultReportingFreqNegativeSeconds() throws Exception {
    Properties p = getDefaultConfigProperties();
    p.setProperty(REPORTING_FREQ_PROPERTY_NAME, "-13.7s");
    ConfigService configService = new ConfigService(p);
    assertEquals(15000L, configService.getReportingFrequency());
  }

  @Test
  public void testReportingFreqMins() throws Exception {
    Properties p = getDefaultConfigProperties();
    p.setProperty(REPORTING_FREQ_PROPERTY_NAME, "1M");
    ConfigService configService = new ConfigService(p);
    assertEquals(60000L, configService.getReportingFrequency());
  }

  @Test
  public void testReportingFreqPartialMins() throws Exception {
    Properties p = getDefaultConfigProperties();
    p.setProperty(REPORTING_FREQ_PROPERTY_NAME, "1.5M");
    ConfigService configService = new ConfigService(p);
    assertEquals(15000L, configService.getReportingFrequency());
  }

  @Test
  public void testReportingFreqMinsAndSeconds() throws Exception {
    Properties p = getDefaultConfigProperties();
    p.setProperty(REPORTING_FREQ_PROPERTY_NAME, "17M-13.7s");
    ConfigService configService = new ConfigService(p);
    assertEquals((17*60*1000-13700), configService.getReportingFrequency());
  }

  @Test
  public void testGetApiUrl() throws Exception {
    Properties p = getDefaultConfigProperties();
    String url = "https://apptuit.ai";
    p.setProperty("apptuit.api_url", url);
    ConfigService configService = new ConfigService(p);
    assertEquals(new URL(url), configService.getApiUrl());
  }

  @Test
  public void testBadApiUrl() throws Exception {
    Properties p = getDefaultConfigProperties();
    String url = "junk";
    p.setProperty("apptuit.api_url", url);
    ConfigService configService = new ConfigService(p);
    assertNull(configService.getApiUrl());
  }

  @Test
  public void testGetTagsMissing() throws Exception {
    Properties p = new Properties();
    p.setProperty(REPORTING_MODE_PROPERTY_NAME, "xcollector");
    ConfigService configService = new ConfigService(p);
    Map<String, String> globalTags = configService.getGlobalTags();
    assertEquals(globalTags, Collections.emptyMap());
  }

  @Test
  public void testGlobalUUIDTag() throws Exception {
    Properties p = new Properties();
    p.setProperty("global_tags", "jvmid:${UUID}");
    ConfigService configService = new ConfigService(p);
    Map<String, String> globalTags = configService.getGlobalTags();
    assertEquals(1, globalTags.size());
    assertEquals(36, globalTags.get("jvmid").length());
  }

  @Test
  public void testGlobalProcessIDTag() throws Exception {
    Properties p = new Properties();
    p.setProperty("global_tags", "pid:${PID}");
    ConfigService configService = new ConfigService(p);
    Map<String, String> globalTags = configService.getGlobalTags();
    assertEquals(1, globalTags.size());
    assertEquals(getThisJVMProcessID() + "", globalTags.get("pid"));
  }

  @Test
  public void testPutReporterHasHostTagAlways() throws Exception {
    Properties p = getApptuitReporterConfigProperties(ReportingMode.API_PUT);
    ConfigService configService = new ConfigService(p);
    Map<String, String> globalTags = configService.getGlobalTags();
    assertEquals(1, globalTags.size());
    assertTrue(globalTags.get("host") != null);
  }

  @Test
  public void testNonPutReporterDoesNotHaveHostTag() throws Exception {
    Properties p = getDefaultConfigProperties();
    ConfigService configService = new ConfigService(p);
    Map<String, String> globalTags = configService.getGlobalTags();
    assertEquals(0, globalTags.size());
    assertTrue(globalTags.get("host") == null);
  }

  @Test
  public void testGlobalTagsOverrideDefaultHostTag() throws Exception {
    Properties p = getDefaultConfigProperties();
    String v1 = UUID.randomUUID().toString();
    String v2 = UUID.randomUUID().toString();
    p.setProperty("global_tags", "host:" + v1 + ", k2 : " + v2);
    ConfigService configService = new ConfigService(p);
    Map<String, String> globalTags = configService.getGlobalTags();
    assertTrue(v1.equals(globalTags.get("host")));
  }


  @Test
  public void testGetTags() throws Exception {
    Properties p = getDefaultConfigProperties();
    String v1 = UUID.randomUUID().toString();
    String v2 = UUID.randomUUID().toString();
    p.setProperty("global_tags", "k1:" + v1 + ", k2 : " + v2);
    ConfigService configService = new ConfigService(p);
    Map<String, String> globalTags = configService.getGlobalTags();
    assertEquals(2, globalTags.size());
    assertEquals(v1, globalTags.get("k1"));
    assertEquals(v2, globalTags.get("k2"));
  }

  @Test(expected = ConfigurationException.class)
  public void testMissingTagValue() throws Exception {
    Properties p = new Properties();
    String v1 = UUID.randomUUID().toString();
    String v2 = UUID.randomUUID().toString();
    p.setProperty("global_tags", "k1: , k2 : " + v2);
    new ConfigService(p);
  }

  @Test(expected = ConfigurationException.class)
  public void testMultipleColons() throws Exception {
    Properties p = new Properties();
    String v1 = UUID.randomUUID().toString();
    String v2 = UUID.randomUUID().toString();
    p.setProperty("global_tags", "k1:" + v1 + " k2 : " + v2);
    new ConfigService(p);
  }

  @Test
  public void testDefaultInstance() throws Exception {
    ConfigService cs = ConfigService.getInstance();
    assertEquals("INTEGRATION_TEST_TOKEN", cs.getApiToken());
    assertEquals("testValue1", cs.getGlobalTags().get("testTag1"));
    assertEquals("testValue2", cs.getGlobalTags().get("testTag2"));
  }

  @Test
  public void testAgentVersion() throws Exception {
    ConfigService cs = ConfigService.getInstance();
    assertEquals(System.getProperty("project.version"), cs.getAgentVersion());
  }

  @SuppressWarnings("all")
  @Test
  public void testLoadSystemProperties() throws Exception {
    // setup here since ConfigService.getInstance() has been initialized in several tests
    System.setProperty("jinsight.reporter", "APPTUIT");
    System.setProperty("apptuit.access_token", "TEST_TOKEN");
    System.setProperty("apptuit.api_url", "http://api.test.bicycle.io");
    System.setProperty("jinsight.global_tags", "env:dev");

    doPrivilegedAction();

    initialize();

    ConfigService cs = ConfigService.getInstance();
    assertEquals("APPTUIT", cs.getReporterType().name());
    assertEquals("TEST_TOKEN", cs.getApiToken());
    assertEquals("http://api.test.bicycle.io", cs.getApiUrl().toString());
    Map<String, String> globalTags = cs.getGlobalTags();
    assertEquals("dev", globalTags.get("env"));

    // clean the setup
    System.setProperty("jinsight.reporter", "");
    System.setProperty("jinsight.apptuit.access_token", "");
    System.setProperty("jinsight.apptuit.api_url", "");
    System.setProperty("jinsight.global_tags", "");
    doPrivilegedAction();
  }

  private void doPrivilegedAction() throws  Exception {
    Field singletonField  = ConfigService.class.getDeclaredField("singleton");
    singletonField.setAccessible(true);
    singletonField.set(null, null);

  }

  private Properties getDefaultConfigProperties() {
    return new Properties();
  }

  private Properties getApptuitReporterConfigProperties(ReportingMode reportingMode) {
    Properties p = new Properties();
    p.setProperty(REPORTER_PROPERTY_NAME, ReporterType.APPTUIT.name());
    p.setProperty(REPORTING_MODE_PROPERTY_NAME, reportingMode.name());
    if (reportingMode == ReportingMode.API_PUT) {
      p.setProperty("apptuit.access_token", UUID.randomUUID().toString());
    }
    return p;
  }
}
