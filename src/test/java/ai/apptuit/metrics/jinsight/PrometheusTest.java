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

import static ai.apptuit.metrics.jinsight.ConfigService.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import ai.apptuit.metrics.dropwizard.ApptuitReporterFactory;
import com.codahale.metrics.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.zip.GZIPInputStream;

import io.prometheus.client.Collector;
import org.junit.Before;
import org.junit.Test;

public class PrometheusTest {
  private ApptuitReporterFactory mockFactory;
  private ConfigService mockConfigService;
  private PromHttpServer mockServer;

  public String readInputStream(InputStream inputStream)
          throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    byte[] buffer = new byte[2048];
    int length;
    while ((length = inputStream.read(buffer)) != -1) {
      byteArrayOutputStream.write(buffer, 0, length);
    }
    return byteArrayOutputStream.toString("UTF-8");
  }

  public String readGzipInputStream(InputStream inputStream) throws IOException {
    GZIPInputStream gis = new GZIPInputStream(inputStream);
    byte[] buffer = new byte[2048];
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    int length;
    while ((length = gis.read(buffer)) != -1) {
      byteArrayOutputStream.write(buffer, 0, length);
    }
    return byteArrayOutputStream.toString("UTF-8");
  }

  @Before
  public void setUp() throws Exception {
    mockFactory = mock(ApptuitReporterFactory.class);
    mockConfigService = mock(ConfigService.class);
    mockServer = mock(PromHttpServer.class);
    when(mockConfigService.getGlobalTags()).thenReturn(ConfigService.getInstance().getGlobalTags());
    when(mockConfigService.getReporterType()).thenReturn(ReporterType.PROMETHEUS);
  }

  @Test
  public void testGetReportingModePrometheus() throws Exception {
    Properties p = new Properties();
    p.setProperty(REPORTER_PROPERTY_NAME, "PROMETHEUS");
    ConfigService configService = new ConfigService(p);
    assertEquals(ReporterType.PROMETHEUS, configService.getReporterType());
  }

  @Test
  public void testGetPrometheusPort() throws Exception {
    Properties p = new Properties();
    int port = new Random().nextInt(1000) + 6000;
    p.setProperty(REPORTER_PROPERTY_NAME, "PROMETHEUS");
    p.setProperty(PROMETHEUS_EXPORTER_PORT, Integer.toString(port));
    ConfigService configService = new ConfigService(p);
    assertEquals(port, configService.getPrometheusPort());
  }

  @Test
  public void testGetPrometheusPortDefault() throws Exception {
    Properties p = new Properties();
    p.setProperty(REPORTER_PROPERTY_NAME, "PROMETHEUS");
    ConfigService configService = new ConfigService(p);
    //default port 9404
    assertEquals(9404, configService.getPrometheusPort());
  }

  @Test
  public void testGetPrometheusPortIlligalValue() throws Exception {
    Properties p = new Properties();
    p.setProperty(REPORTER_PROPERTY_NAME, "PROMETHEUS");
    p.setProperty(PROMETHEUS_EXPORTER_PORT, "12aab"); //random string
    ConfigService configService = new ConfigService(p);
    //default port 9404
    assertEquals(9404, configService.getPrometheusPort());
  }

  @Test
  public void testGetPrometheusMetricsPathDefault() throws Exception {
    Properties p = new Properties();
    p.setProperty(REPORTER_PROPERTY_NAME, "PROMETHEUS");
    ConfigService configService = new ConfigService(p);
    //default port 9404
    assertEquals("/metrics", configService.getPrometheusMetricsPath());
  }

  @Test
  public void testGetPrometheusMetricsPath() throws Exception {
    Properties p = new Properties();
    p.setProperty(REPORTER_PROPERTY_NAME, "PROMETHEUS");
    p.setProperty(PROMETHEUS_METRICS_PATH, "/temp");
    ConfigService configService = new ConfigService(p);
    //default port 9404
    assertEquals("/temp", configService.getPrometheusMetricsPath());
  }

  @Test
  public void testPrometheusServerCall() throws Exception {
    Properties p = new Properties();
    p.setProperty(REPORTER_PROPERTY_NAME, "PROMETHEUS");
    ConfigService configService = new ConfigService(p);
    new RegistryService(configService, mockFactory);
    // if no build is called then it is in else block of the RegistryService i.e, promServer
    verify(mockFactory, never()).build(any(MetricRegistry.class));

  }

  @Test
  public void testPrometheusServer() throws Exception {
    Properties p = new Properties();
    p.setProperty(REPORTER_PROPERTY_NAME, "PROMETHEUS");
    ConfigService configService = new ConfigService(p);
    new RegistryService(configService, mockFactory);
    try {
      URL url = new URL("http://localhost:9404/metrics");
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("GET");
      connection.connect();

      int code = connection.getResponseCode();
      String upTimeMetricString = "jvm_uptime_millis";
      String responseString = readInputStream(connection.getInputStream());
      assertEquals(code, 200);
      assertNotEquals(-1, responseString.indexOf(upTimeMetricString));
    } catch (Exception e) {
      assert false;
    }
    // if server is successfully created then there will not be any exception
    assert true;
  }


  @Test
  public void testPrometheusServerGZipRequest() throws Exception {
    Properties p = new Properties();
    p.setProperty(REPORTER_PROPERTY_NAME, "PROMETHEUS");
    ConfigService configService = new ConfigService(p);
    new RegistryService(configService, mockFactory);
    try {
      URL url = new URL("http://localhost:9404/metrics");
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestProperty("Accept-Encoding", "gzip");
      connection.setRequestMethod("GET");
      connection.connect();

      int code = connection.getResponseCode();
      String upTimeMetricString = "jvm_uptime_millis";
      String responseString = readGzipInputStream(connection.getInputStream());
      //System.out.println(responseString);
      assertEquals(code, 200);
      assertNotEquals(-1, responseString.indexOf(upTimeMetricString));
    } catch (Exception e) {
      assert false;
    }
    // if server is successfully created then there will not be any exception
    assert true;
  }


  @Test
  public void testCustomSampleBuilderNullGlobalTag() throws Exception {
    ArrayList<String> e = new ArrayList<>();
    ArrayList<String> labels = new ArrayList<>(Arrays.asList("label_1", "label_2"));
    ArrayList<String> values = new ArrayList<>(Arrays.asList("value.1", "value.2"));
    String dropwizardName1 = "sample.metric[label.1:value.1,label.2:value.2]";
    double b = 2.0;
    e.clear();
    TagDecodingSampleBuilder customSampleBuilder = new TagDecodingSampleBuilder(null);
    Collector.MetricFamilySamples.Sample sample = customSampleBuilder.createSample(dropwizardName1, "", e, e, b);
    assert sample.name.equals("sample_metric");
    assert sample.labelNames.equals(labels);
    assert sample.labelValues.equals(values);
  }

  @Test
  public void testCustomSampleBuilderGlobalTag() throws Exception {
    //global_tags=host:junit, testTag1:testValue1, testTag2:testValue2
    ArrayList<String> e = new ArrayList<>();
    ArrayList<String> labels = new ArrayList<>(Arrays.asList("label_1", "label_2", "host", "testTag1", "testTag2"));
    ArrayList<String> values = new ArrayList<>(Arrays.asList("value.1", "value.2", "junit", "testValue1", "testValue2"));
    String dropwizardName1 = "sample.metric[label.1:value.1,label.2:value.2]";
    double b = 2.0;
    e.clear();
    TagDecodingSampleBuilder customSampleBuilder = new TagDecodingSampleBuilder(mockConfigService.getGlobalTags());
    Collector.MetricFamilySamples.Sample sample = customSampleBuilder.createSample(dropwizardName1, "", e, e, b);
    assert sample.name.equals("sample_metric");
    assert sample.labelNames.equals(labels);
    assert sample.labelValues.equals(values);
  }

  @Test
  public void testCustomSampleBuilderGlobalTagOverlapping() throws Exception {
    //global_tags=host:junit, testTag1:testValue1, testTag2:testValue2
    ArrayList<String> e = new ArrayList<>();
    ArrayList<String> labels = new ArrayList<>(Arrays.asList("host_1", "label_2", "testTag1", "testTag2"));
    ArrayList<String> values = new ArrayList<>(Arrays.asList("local", "value.2", "testValue1", "testValue2"));
    String dropwizardName1 = "sample.metric[host.1:local,label.2:value.2]";
    Map<String, String> globalTags = new HashMap<>();
    globalTags.put("host_1", "JIhost");
    globalTags.put("testTag1", "testValue1");
    globalTags.put("testTag2", "testValue2");
    double b = 2.0;
    e.clear();
    TagDecodingSampleBuilder customSampleBuilder = new TagDecodingSampleBuilder(globalTags);
    Collector.MetricFamilySamples.Sample sample = customSampleBuilder.createSample(dropwizardName1, "", e, e, b);
    assert sample.name.equals("sample_metric");
    assert sample.labelNames.equals(labels);
    assert sample.labelValues.equals(values);
  }

  //to test the startup message for Prom And apptuit
  @Test
  public void testPrometheusStartUpMessage() throws Exception {
    Properties p = new Properties();
    p.setProperty(REPORTER_PROPERTY_NAME, "PROMETHEUS");
    ConfigService configService = new ConfigService(p);
    String actualMessage = Agent.getStartupMessage(configService);

    String expectedMessage = "JInsight v[" + configService.getAgentVersion() + "] initialized with reporter ["
            + configService.getReporterType() + "]. ";
    expectedMessage = expectedMessage + "Using port[" + configService.getPrometheusPort()
            + "] on metrics path [" + configService.getPrometheusMetricsPath() + "].";
    //System.out.println(actualMessage);
    assertEquals(expectedMessage, actualMessage);
  }

  @Test
  public void testPrometheusStartUpMessageError() throws Exception {
    when(mockConfigService.getReporterType()).thenReturn(null);
    try {
      String actualMessage = Agent.getStartupMessage(mockConfigService);
    } catch (IllegalStateException e) {
      //as mockConfigService returns null for the getReporterType we should ideally get the IllegalStateException
      assert true;
      return;
    }
    catch (Exception e) {
      assert false;
    }
    assert false;
  }

  //configService readReporterTest
  @Test
  public void testGetConfigReadReporterTypeInvalid() throws Exception {
    Properties p = new Properties();
    p.setProperty("apptuit.access_token", UUID.randomUUID().toString());
    p.setProperty(REPORTER_PROPERTY_NAME, "InvalidReporter");
    ConfigService configService = new ConfigService(p);
    assertEquals(DEFAULT_REPORTER_TYPE, configService.getReporterType());
  }

  @Test
  public void testGetConfigReadReporterTypeEmptyString() throws Exception {
    Properties p = new Properties();
    p.setProperty("apptuit.access_token", UUID.randomUUID().toString());
    p.setProperty(REPORTER_PROPERTY_NAME, "");
    ConfigService configService = new ConfigService(p);
    assertEquals(DEFAULT_REPORTER_TYPE, configService.getReporterType());
  }
}
