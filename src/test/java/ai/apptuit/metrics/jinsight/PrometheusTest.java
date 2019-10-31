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

import static ai.apptuit.metrics.jinsight.ConfigService.DEFAULT_REPORTER_TYPE;
import static ai.apptuit.metrics.jinsight.ConfigService.PROMETHEUS_EXPORTER_PORT;
import static ai.apptuit.metrics.jinsight.ConfigService.PROMETHEUS_METRICS_PATH;
import static ai.apptuit.metrics.jinsight.ConfigService.REPORTER_PROPERTY_NAME;
import static ai.apptuit.metrics.jinsight.ConfigService.ReporterType;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.apptuit.metrics.client.DataPoint;
import ai.apptuit.metrics.client.TagEncodedMetricName;
import ai.apptuit.metrics.dropwizard.ApptuitReporter;
import ai.apptuit.metrics.dropwizard.ApptuitReporterFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Timer;
import io.prometheus.client.Collector;
import io.prometheus.client.dropwizard.DropwizardExports;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;


public class PrometheusTest {

  private ApptuitReporterFactory mockFactory;
  private ConfigService mockConfigService;
  private PromHttpServer mockServer;
  private static final int UDP_PORT = 8953;

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
    URL url = new URL("http://localhost:9404/metrics");
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("GET");
    connection.connect();

    int code = connection.getResponseCode();
    String upTimeMetricString = "jvm_uptime_seconds";
    String responseString = readInputStream(connection.getInputStream());
    assertEquals(code, 200);
    assertNotEquals(-1, responseString.indexOf(upTimeMetricString));
  }


  @Test
  public void testPrometheusServerGZipRequest() throws Exception {
    Properties p = new Properties();
    p.setProperty(REPORTER_PROPERTY_NAME, "PROMETHEUS");
    ConfigService configService = new ConfigService(p);
    new RegistryService(configService, mockFactory);
    URL url = new URL("http://localhost:9404/metrics");
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestProperty("Accept-Encoding", "gzip");
    connection.setRequestMethod("GET");
    connection.connect();

    int code = connection.getResponseCode();
    String upTimeMetricString = "jvm_uptime_seconds";
    String responseString = readGzipInputStream(connection.getInputStream());
    //System.out.println(responseString);
    assertEquals(code, 200);
    assertNotEquals(-1, responseString.indexOf(upTimeMetricString));
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
    assertEquals("sample_metric", sample.name);
    assertEquals(labels, sample.labelNames);
    assertEquals(values, sample.labelValues);
  }

  @Test
  public void testCustomSampleBuilderGlobalTag() throws Exception {
    //global_tags=host:junit, testTag1:testValue1, testTag2:testValue2
    ArrayList<String> e = new ArrayList<>();
    ArrayList<String> labels = new ArrayList<>(Arrays.asList("label_1", "label_2", "host", "testTag1", "testTag2"));
    ArrayList<String> values = new ArrayList<>(
        Arrays.asList("value.1", "value.2", "junit", "testValue1", "testValue2"));
    String dropwizardName1 = "sample.metric[label.1:value.1,label.2:value.2]";
    double b = 2.0;
    e.clear();
    TagDecodingSampleBuilder customSampleBuilder = new TagDecodingSampleBuilder(mockConfigService.getGlobalTags());
    Collector.MetricFamilySamples.Sample sample = customSampleBuilder.createSample(dropwizardName1, "", e, e, b);
    assertEquals("sample_metric", sample.name);
    assertEquals(labels, sample.labelNames);
    assertEquals(values, sample.labelValues);
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
    assertEquals("sample_metric", sample.name);
    assertEquals(labels, sample.labelNames);
    assertEquals(values, sample.labelValues);
  }

  //to test the startup message for Prom And apptuit
  @Test
  public void testPrometheusStartUpMessage() throws Exception {
    Properties p = new Properties();
    p.setProperty(REPORTER_PROPERTY_NAME, "PROMETHEUS");
    ConfigService configService = new ConfigService(p);
    String actualMessage = Agent.getStartupMessage(configService, true);
    assertThat(actualMessage, CoreMatchers.containsString("v[" + configService.getAgentVersion() + "]"));
    assertThat(actualMessage, CoreMatchers.containsString(configService.getReporterType().toString()));
    assertThat(actualMessage, CoreMatchers.containsString(String.valueOf(configService.getPrometheusPort())));
    assertThat(actualMessage, CoreMatchers.containsString(configService.getPrometheusMetricsPath()));
    assertThat(actualMessage, CoreMatchers.endsWith("SUCCESSFUL"));
  }

  @Test(expected = IllegalStateException.class)
  public void testPrometheusWrongReporterType() throws Exception {
    when(mockConfigService.getReporterType()).thenReturn(null);
    String actualMessage = Agent.getStartupMessage(mockConfigService, false);
  }

  @Test
  public void testPrometheusStartUpMessageError() throws Exception {
    String actualMessage = Agent.getStartupMessage(mockConfigService, false);
    assertThat(actualMessage, CoreMatchers.endsWith("FAILED"));
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

  @Test
  public void testPrometheusAndApptuitAreIdentical() throws Exception {

    final MetricRegistry metricRegistry = new MetricRegistry();

    TagEncodedMetricName gaugeName = TagEncodedMetricName.decode("test.metric.gauge").withTags("env", "test");
    TagEncodedMetricName counterName = TagEncodedMetricName.decode("test.metric.counter").withTags("env", "test");
    TagEncodedMetricName meterName = TagEncodedMetricName.decode("test.metric.meter").withTags("env", "test");
    TagEncodedMetricName histName = TagEncodedMetricName.decode("test.metric.histogram").withTags("env", "test");
    TagEncodedMetricName timerName = TagEncodedMetricName.decode("test.metric.timer").withTags("env", "test");

    metricRegistry.register(gaugeName.toString(), (Gauge<Integer>) () -> 120);

    Counter counter = metricRegistry.counter(counterName.toString());
    counter.inc();

    metricRegistry.meter(meterName.toString()).mark();

    Histogram histogram = metricRegistry.histogram(histName.toString());
    histogram.update(10);

    Timer timer = metricRegistry.timer(timerName.toString());
    timer.update(20, TimeUnit.MILLISECONDS);

    TagDecodingSampleBuilder builder = new TagDecodingSampleBuilder(null);
    ApptuitDropwizardExports exporter = new ApptuitDropwizardExports(metricRegistry, builder);
    Set<TagEncodedMetricName> promDatapoints = getTagEncodedMetricNames(exporter);

    Set<TagEncodedMetricName> origPromDatapoints = getTagEncodedMetricNames(
        new DropwizardExports(metricRegistry, builder));
//    System.out.println(origPromDatapoints);

    MockServer mockServer = new MockServer();
    mockServer.start();

    ApptuitReporterFactory factory = new ApptuitReporterFactory();
    factory.setReportingMode(ApptuitReporter.ReportingMode.XCOLLECTOR);
    ScheduledReporter reporter = factory.build(metricRegistry);
    reporter.start(15, TimeUnit.SECONDS);

    await().atMost(50, TimeUnit.SECONDS).until(() -> mockServer.countReceivedDPs() >= 30);

    DataPoint[] dataPoints = mockServer.getReceivedDPs();
    mockServer.stop();

    HashSet<TagEncodedMetricName> apptuitTimeseries = new HashSet<>();
    Map<String, String> tags;

    for (DataPoint dataPoint : dataPoints) {
      String apptuitName = dataPoint.getMetric();

      if (!apptuitName.startsWith("apptuit_reporter")) {
        tags = dataPoint.getTags();
        TagEncodedMetricName metric = TagEncodedMetricName.decode(apptuitName).withTags(tags);
        apptuitTimeseries.add(metric);
      }
    }
//    System.out.println(apptuitTimeseries);

    assertEquals(apptuitTimeseries, promDatapoints);

  }

  private Set<TagEncodedMetricName> getTagEncodedMetricNames(Collector exporter) {
    List<Collector.MetricFamilySamples> samples = exporter.collect();

    Set<TagEncodedMetricName> promDatapoints = new HashSet<>();
    List<String> tempLabels, tempValues;

    for (Collector.MetricFamilySamples sample : samples) {
      for (Collector.MetricFamilySamples.Sample sample_metric : sample.samples) {
//        public DataPoint(String metricName, long epoch, Number value, Map<String, String> tags) {
        Map<String, String> tags = new HashMap<>();
        String promName = sample_metric.name;
        tempLabels = sample_metric.labelNames;
        tempValues = sample_metric.labelValues;

        for (int i = 0; i < tempLabels.size(); i++) {
          tags.put(tempLabels.get(i), tempValues.get(i));
        }
        TagEncodedMetricName metric = TagEncodedMetricName.decode(promName).withTags(tags);
        promDatapoints.add(metric);
      }
    }
    return promDatapoints;
  }

  private static class MockServer {

    private final DatagramSocket socket;
    private final byte[] buf = new byte[8192];
    private final Thread thread;
    private final List<DataPoint> receivedDPs = new ArrayList<>();
    private boolean running = true;

    public MockServer() throws SocketException {
      socket = new DatagramSocket(UDP_PORT);
      thread = new Thread(new RequestProcessor());
      thread.setDaemon(true);
    }

    public void start() {
      thread.start();
    }

    public void stop() {
      running = false;
      thread.interrupt();
    }

    public int countReceivedDPs() {
      return receivedDPs.size();
    }

    public DataPoint[] getReceivedDPs() {
      return receivedDPs.toArray(new DataPoint[receivedDPs.size()]);
    }

    public void clearReceivedDPs() {
      receivedDPs.clear();
    }

    private class RequestProcessor implements Runnable {

      @Override
      public void run() {
        while (running) {
          DatagramPacket packet = new DatagramPacket(buf, buf.length);
          try {
            socket.receive(packet);
            String data = new String(packet.getData(), 0, packet.getLength());
            //System.err.printf("Got packet of [%d] bytes.\n", data.length());
            Scanner lines = new Scanner(data).useDelimiter("\n");
//            lines.forEachRemaining(line -> receivedDPs.add(toDataPoint(line)));
            while (lines.hasNext()) {
              receivedDPs.add(toDataPoint(lines.nextLine()));
            }
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }

      private DataPoint toDataPoint(String line) {
        //DataPoint(String metricName, long epoch, Number value, Map<String, String> tags)
        String[] fields = line.split(" ");
        return new DataPoint(fields[0], Long.parseLong(fields[1]),
            Double.parseDouble(fields[2]), getTags(fields));
      }

      private Map<String, String> getTags(String[] fields) {
        Map<String, String> retVal = new HashMap<>();
        for (int i = 3; i < fields.length; i++) {
          String[] tag = fields[i].split("=");
          retVal.put(tag[0], tag[1]);
        }
        return retVal;
      }
    }
  }

}