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

package ai.apptuit.metrics.client;

import static org.junit.Assert.assertEquals;

import ai.apptuit.metrics.client.ApptuitPutClient.DatapointsHttpEntity;
import ai.apptuit.metrics.dropwizard.TagEncodedMetricName;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.zip.GZIPInputStream;
import org.json.simple.parser.ParseException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Rajiv Shivane
 */

public class ApptuitPutClientTest {

  private static MockServer httpServer;

  private TagEncodedMetricName tagEncodedMetricName;
  private HashMap<String, String> globalTags;

  private static String streamToString(InputStream inputStream) throws IOException {
    return new Scanner(new GZIPInputStream(inputStream)).useDelimiter("\0").next();
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
    httpServer = new MockServer();
    httpServer.start();
  }

  @AfterClass
  public static void tearDownClass() throws Exception {
    httpServer.stop(0);
  }

  @Before
  public void setUp() throws Exception {
    tagEncodedMetricName = TagEncodedMetricName.decode("proc.stat.cpu")
        .withTags("type", "idle");

    globalTags = new HashMap<>();
    globalTags.put("host", "rajiv");
    globalTags.put("env", "dev");
    globalTags.put("dev", "rajiv");
  }

  @After
  public void tearDown() throws Exception {
    httpServer.resetCapturedData();
  }


  @Test
  public void testEntityMarshallingNoGZIP() throws Exception {
    for (int numDataPoints = 1; numDataPoints <= 10; numDataPoints++) {
      ArrayList<DataPoint> dataPoints = createDataPoints(numDataPoints);
      DatapointsHttpEntity entity = new DatapointsHttpEntity(dataPoints, globalTags, false);

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      entity.writeTo(baos);
      DataPoint[] unmarshalledDPs = Util.jsonToDataPoints(new String(baos.toByteArray()));

      assertEquals(numDataPoints, unmarshalledDPs.length);
      for (int i = 0; i < numDataPoints; i++) {
        assertEquals(getExpectedDataPoint(dataPoints.get(i), globalTags), unmarshalledDPs[i]);
      }
    }
  }

  @Test
  public void testEntityMarshallingWithGZIP() throws Exception {
    for (int numDataPoints = 1; numDataPoints <= 10; numDataPoints++) {
      ArrayList<DataPoint> dataPoints = createDataPoints(numDataPoints);
      DatapointsHttpEntity entity = new DatapointsHttpEntity(dataPoints, globalTags, true);

      PipedInputStream pis = new PipedInputStream();

      entity.writeTo(new PipedOutputStream(pis));

      String jsonText = streamToString(pis);
      DataPoint[] unmarshalledDPs = Util.jsonToDataPoints(jsonText);

      assertEquals(numDataPoints, unmarshalledDPs.length);
      for (int i = 0; i < numDataPoints; i++) {
        assertEquals(getExpectedDataPoint(dataPoints.get(i), globalTags), unmarshalledDPs[i]);
      }
    }
  }

  @Test
  public void testPut200() throws Exception {
    testPut(200);
  }

  @Test
  public void testPut400() throws Exception {
    testPut(400);
  }

  private void testPut(int status) throws MalformedURLException, ParseException {
    //Util.enableHttpClientTracing();

    int numDataPoints = 10;
    ArrayList<DataPoint> dataPoints = createDataPoints(numDataPoints);

    URL apiEndPoint = httpServer.getUrl(status);
    ApptuitPutClient client = new ApptuitPutClient(MockServer.token, globalTags, apiEndPoint);
    client.put(dataPoints);

    List<HttpExchange> exchanges = httpServer.getExchanges();
    List<String> requestBodies = httpServer.getRequestBodies();

    HttpExchange exchange = exchanges.get(0);
    assertEquals("POST", exchange.getRequestMethod());
    assertEquals(MockServer.path, exchange.getRequestURI().getPath());

    Headers headers = exchange.getRequestHeaders();
    assertEquals("gzip", headers.getFirst("Content-Encoding"));
    assertEquals("application/json", headers.getFirst("Content-Type"));
    assertEquals("Bearer " + MockServer.token, headers.getFirst("Authorization"));

    DataPoint[] unmarshalledDPs = Util.jsonToDataPoints(requestBodies.get(0));

    assertEquals(numDataPoints, unmarshalledDPs.length);
    for (int i = 0; i < numDataPoints; i++) {
      assertEquals(getExpectedDataPoint(dataPoints.get(i), globalTags), unmarshalledDPs[i]);
    }
  }

  private ArrayList<DataPoint> createDataPoints(int numDataPoints) {
    ArrayList<DataPoint> dataPoints = new ArrayList<>(numDataPoints);
    for (int i = 0; i < numDataPoints; i++) {
      long value = 99 + i;
      long epoch = System.currentTimeMillis() / 1000 - value;
      DataPoint dataPoint = new DataPoint(tagEncodedMetricName.getMetricName(), epoch, value,
          tagEncodedMetricName.getTags());
      dataPoints.add(dataPoint);
    }
    return dataPoints;
  }

  private DataPoint getExpectedDataPoint(DataPoint dataPoint, HashMap<String, String> globalTags) {
    Map<String, String> tags = new HashMap<>(dataPoint.getTags());
    tags.putAll(globalTags);
    return new DataPoint(dataPoint.getMetric(), dataPoint.getTimestamp(), dataPoint.getValue(),
        tags);
  }

  private static class MockServer {

    private static final int port = 9797;
    private static final String path = "/api/endpoint";
    private static final String token = "MOCK_APPTUIT_TOKEN";
    private static final String SUCCESS_RESPONSE_BODY = "{\"success\":1,\"failed\":0,\"errors\":[]}";
    private static final String STATUS400_RESPONSE_BODY = "{\"success\":223,\"failed\":2,\"errors\":[{\"datapoint\":{\"metric\":\"tomcat.requests.duration.mean\",\"timestamp\":1513650393,\"value\":\"NaN\",\"tags\":{\"method\":\"DELETE\",\"context\":\"ROOT\",\"host\":\"ade-instance.c.pivotal-canto-171605.internal\",\"env\":\"dev\",\"collector\":\"jinsight\",\"status\":\"200\"}},\"error\":\"Unable to parse value to a number\"},{\"datapoint\":{\"metric\":\"tomcat.requests.duration.mean\",\"timestamp\":1513650393,\"value\":\"NaN\",\"tags\":{\"method\":\"POST\",\"context\":\"ROOT\",\"host\":\"ade-instance.c.pivotal-canto-171605.internal\",\"env\":\"dev\",\"collector\":\"jinsight\",\"status\":\"200\"}},\"error\":\"Unable to parse value to a number\"}]}";

    private HttpServer httpServer;
    private List<HttpExchange> exchanges = new ArrayList<>();
    private List<String> requestBodies = new ArrayList<>();

    public MockServer() throws IOException {
      httpServer = HttpServer.create(new InetSocketAddress(port), 0);
      httpServer.createContext(path, this::handleExchange);
    }

    public void start() {
      httpServer.start();
    }

    public void stop(int i) {
      httpServer.stop(i);
    }

    private URL getUrl() throws MalformedURLException {
      return getUrl(HttpURLConnection.HTTP_OK);
    }

    private URL getUrl(int code) throws MalformedURLException {
      String url = "http://localhost:" + port + path;
      if (code != 200) {
        url += "?status=" + code;
      }
      return new URL(url);
    }

    public List<HttpExchange> getExchanges() {
      return exchanges;
    }

    public List<String> getRequestBodies() {
      return requestBodies;
    }

    public void resetCapturedData() {
      exchanges.clear();
      requestBodies.clear();
    }

    private void handleExchange(HttpExchange exchange) throws IOException {
      exchanges.add(exchange);
      requestBodies.add(streamToString(exchange.getRequestBody()));

      int status = getResponseType(exchange);
      byte[] response = null;
      switch (status) {
        case HttpURLConnection.HTTP_BAD_REQUEST:
          response = STATUS400_RESPONSE_BODY.getBytes();
          break;
        default:
          response = SUCCESS_RESPONSE_BODY.getBytes();
          status = HttpURLConnection.HTTP_OK;
          break;
      }
      exchange.sendResponseHeaders(status, response.length);
      exchange.getResponseBody().write(response);
      exchange.close();
    }

    private int getResponseType(HttpExchange exchange) {
      URI uri = exchange.getRequestURI();
      String rawQuery = uri.getRawQuery();
      if (rawQuery == null) {
        return HttpURLConnection.HTTP_OK;
      }
      if ("status=400".equals(rawQuery)) {
        return HttpURLConnection.HTTP_BAD_REQUEST;
      }
      return HttpURLConnection.HTTP_OK;
    }
  }

}
