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
import static org.junit.Assert.assertNull;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.zip.GZIPInputStream;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Rajiv Shivane
 */

public class ApptuitPutClientTest {

  private TagEncodedMetricName tagEncodedMetricName;
  private HashMap<String, String> globalTags;

  @Before
  public void setUp() throws Exception {
    tagEncodedMetricName = TagEncodedMetricName.decode("proc.stat.cpu")
        .submetric(null, "type", "idle");

    globalTags = new HashMap<>();
    globalTags.put("host", "rajiv");
    globalTags.put("env", "dev");
    globalTags.put("dev", "rajiv");

  }


  @Test
  public void testEntityMarshallingNoGZIP() throws Exception {
    for (int numDataPoints = 1; numDataPoints <= 10; numDataPoints++) {
      ArrayList<DataPoint> dataPoints = createDataPoints(numDataPoints);
      DatapointsHttpEntity entity = new DatapointsHttpEntity(dataPoints, globalTags, false);
      assertEquals(DatapointsHttpEntity.APPLICATION_JSON, entity.getContentType().getValue());
      assertNull(entity.getContentEncoding());

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

      assertEquals(DatapointsHttpEntity.APPLICATION_JSON, entity.getContentType().getValue());
      assertEquals(DatapointsHttpEntity.CONTENT_ENCODING_GZIP,
          entity.getContentEncoding().getValue());

      String jsonText = streamToString(pis);
      DataPoint[] unmarshalledDPs = Util.jsonToDataPoints(jsonText);

      assertEquals(numDataPoints, unmarshalledDPs.length);
      for (int i = 0; i < numDataPoints; i++) {
        assertEquals(getExpectedDataPoint(dataPoints.get(i), globalTags), unmarshalledDPs[i]);
      }
    }
  }

  @Test
  public void testPut() throws Exception {
    //Util.enableHttpClientTracing();

    int numDataPoints = 10;
    ArrayList<DataPoint> dataPoints = createDataPoints(numDataPoints);

    int port = 9797;
    String path = "/api/endpoint";
    String token = "MOCK_APPTUIT_TOKEN";

    HttpServer httpServer = HttpServer.create(new InetSocketAddress(port), 0);

    List<HttpExchange> exchanges = new ArrayList<>();
    List<String> requestBodies = new ArrayList<>();
    httpServer.createContext(path, exchange -> {
      exchanges.add(exchange);
      requestBodies.add(streamToString(exchange.getRequestBody()));

      byte[] response = "{\"success\":1,\"failed\":0,\"errors\":[]}".getBytes();
      exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
      exchange.getResponseBody().write(response);
      exchange.close();
    });

    httpServer.start();

    try {

      String apiEndPoint = "http://localhost:" + port + path;
      ApptuitPutClient client = new ApptuitPutClient(token, globalTags,
          apiEndPoint);
      client.put(dataPoints);

    } finally {
      httpServer.stop(0);
    }

    HttpExchange exchange = exchanges.get(0);
    assertEquals("POST", exchange.getRequestMethod());
    assertEquals(path, exchange.getRequestURI().getPath());

    Headers headers = exchange.getRequestHeaders();
    assertEquals("gzip", headers.getFirst("Content-Encoding"));
    assertEquals("application/json", headers.getFirst("Content-Type"));
    assertEquals("Bearer " + token, headers.getFirst("Authorization"));

    DataPoint[] unmarshalledDPs = Util.jsonToDataPoints(requestBodies.get(0));

    assertEquals(numDataPoints, unmarshalledDPs.length);
    for (int i = 0; i < numDataPoints; i++) {
      assertEquals(getExpectedDataPoint(dataPoints.get(i), globalTags), unmarshalledDPs[i]);
    }
  }

  private String streamToString(InputStream inputStream) throws IOException {
    return new Scanner(new GZIPInputStream(inputStream)).useDelimiter("\0").next();
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

}
