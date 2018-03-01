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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import ai.apptuit.metrics.dropwizard.ApptuitReporter.ReportingMode;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import org.junit.Test;

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
    ConfigService configService = new ConfigService(p);
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
    p.setProperty("apptuit.reporting_mode", "junk");
    ConfigService configService = new ConfigService(p);
    assertEquals(ReportingMode.API_PUT, configService.getReportingMode());
  }

  @Test
  public void testGetReportingModeNoOp() throws Exception {
    Properties p = new Properties();
    p.setProperty("apptuit.reporting_mode", "NO_OP");
    ConfigService configService = new ConfigService(p);
    assertEquals(ReportingMode.NO_OP, configService.getReportingMode());
  }

  @Test
  public void testGetReportingModeNoOpCaseInsensitive() throws Exception {
    Properties p = new Properties();
    p.setProperty("apptuit.reporting_mode", "No_Op");
    ConfigService configService = new ConfigService(p);
    assertEquals(ReportingMode.NO_OP, configService.getReportingMode());
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
    p.setProperty("apptuit.reporting_mode", "xcollector");
    ConfigService configService = new ConfigService(p);
    Map<String, String> globalTags = configService.getGlobalTags();
    assertEquals(globalTags, Collections.emptyMap());
  }

  @Test
  public void testPutReporterHasHostTagAlways() throws Exception {
    Properties p = getDefaultConfigProperties();
    ConfigService configService = new ConfigService(p);
    Map<String, String> globalTags = configService.getGlobalTags();
    assertEquals(1, globalTags.size());
    assertTrue(globalTags.get("host") != null);
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
    assertEquals(3, globalTags.size());
    assertEquals(v1, globalTags.get("k1"));
    assertEquals(v2, globalTags.get("k2"));
    assertNotNull(globalTags.get("host"));
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

  private Properties getDefaultConfigProperties() {
    Properties p = new Properties();
    p.setProperty("apptuit.access_token", UUID.randomUUID().toString());
    return p;
  }
}
