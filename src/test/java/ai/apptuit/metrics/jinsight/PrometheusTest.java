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

import static ai.apptuit.metrics.jinsight.ConfigService.PROMETHEUS_EXPORTER_PORT;
import static ai.apptuit.metrics.jinsight.ConfigService.REPORTING_MODE_PROPERTY_NAME;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import ai.apptuit.metrics.dropwizard.ApptuitReporterFactory;
import com.codahale.metrics.*;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Random;

import io.prometheus.client.Collector;
import org.junit.Before;
import org.junit.Test;

public class PrometheusTest {
  private ApptuitReporterFactory mockFactory;
  private ConfigService mockConfigService;
  private PromHttpServer mockServer;
  @Before
  public void setUp() throws Exception {
    mockFactory = mock(ApptuitReporterFactory.class);
    mockConfigService = mock(ConfigService.class);
    mockServer = mock(PromHttpServer.class);
    when(mockConfigService.getGlobalTags()).thenReturn(ConfigService.getInstance().getGlobalTags());
  }

  @Test
  public void testGetReportingModePrometheus() throws Exception {
    Properties p = new Properties();
    p.setProperty(REPORTING_MODE_PROPERTY_NAME, "PROMETHEUS");
    ConfigService configService = new ConfigService(p);
    assertEquals(null, configService.getReportingMode());
  }

  @Test
  public void testGetPrometheusPort() throws Exception {
    Properties p = new Properties();
    int port = new Random().nextInt(1000) + 6000;
    p.setProperty(REPORTING_MODE_PROPERTY_NAME, "PROMETHEUS");
    p.setProperty(PROMETHEUS_EXPORTER_PORT, Integer.toString(port));
    ConfigService configService = new ConfigService(p);
    assertEquals(port, configService.getPrometheusPort());
  }

  @Test
  public void testGetPrometheusPortDefault() throws Exception {
    Properties p = new Properties();
    p.setProperty(REPORTING_MODE_PROPERTY_NAME, "PROMETHEUS");
    ConfigService configService = new ConfigService(p);
    //default port 9404
    assertEquals(9404, configService.getPrometheusPort());
  }

  @Test
  public void testGetPrometheusPortIlligalValue() throws Exception {
    Properties p = new Properties();
    p.setProperty(REPORTING_MODE_PROPERTY_NAME, "PROMETHEUS");
    p.setProperty(PROMETHEUS_EXPORTER_PORT, "12aab"); //random string
    ConfigService configService = new ConfigService(p);
    //default port 9404
    assertEquals(9404, configService.getPrometheusPort());
  }

  @Test
  public void testGetPrometheusMetricsPathDefault() throws Exception {
    Properties p = new Properties();
    p.setProperty(REPORTING_MODE_PROPERTY_NAME, "PROMETHEUS");
    ConfigService configService = new ConfigService(p);
    //default port 9404
    assertEquals("/metrics", configService.getprometheusMetricsPath());
  }

  @Test
  public void testPrometheusServerCall() throws Exception {
    Properties p = new Properties();
    p.setProperty(REPORTING_MODE_PROPERTY_NAME, "PROMETHEUS");
    ConfigService configService = new ConfigService(p);
    new RegistryService(configService,mockFactory);
    // if no build is called then it is in else block of the RegistryService i.e, promServer
    verify(mockFactory,never()).build(any(MetricRegistry.class));

  }

  @Test
  public void testPrometheusServer() throws Exception {
    Properties p = new Properties();
    p.setProperty(REPORTING_MODE_PROPERTY_NAME, "PROMETHEUS");
    ConfigService configService = new ConfigService(p);
    new RegistryService(configService,mockFactory);
    try {
      URL url = new URL("http://localhost:9404/metrics");
      HttpURLConnection connection = (HttpURLConnection)url.openConnection();
      connection.setRequestMethod("GET");
      connection.connect();

      int code = connection.getResponseCode();
      assertEquals(code,200);
    }catch (Exception e)
    {
      assert false;
    }
    // if server is successfully created then there will not be any exception
    assert true;
  }


  @Test
  public void testCustomSampleBuilder() throws Exception {
    ArrayList<String> e = new ArrayList<>();
    ArrayList<String> labels = new ArrayList<>();
    ArrayList<String> values = new ArrayList<>();
    double b = 2.0;
    String dropwizardName1 = "sample.metric[label.1:value.1,label.2:value.2]";
    labels.add("label_1");
    labels.add("label_2");
    values.add("value.1");
    values.add("value.2");
    e.clear();
    CustomMetricBuilderFromDropWizardName customSampleBuilder=new CustomMetricBuilderFromDropWizardName();
    Collector.MetricFamilySamples.Sample sample = customSampleBuilder.createSample(dropwizardName1,"",e,e,b);
    assert sample.name.equals("sample_metric");
    assert sample.labelNames.equals(labels);
    assert sample.labelValues.equals(values);
  }
}