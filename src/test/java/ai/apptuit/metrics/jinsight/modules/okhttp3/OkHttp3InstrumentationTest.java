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

package ai.apptuit.metrics.jinsight.modules.okhttp3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import ai.apptuit.metrics.jinsight.RegistryService;
import ai.apptuit.metrics.jinsight.testing.CountTracker;
import ai.apptuit.metrics.jinsight.testing.CountTracker.Snapshot;
import ai.apptuit.metrics.jinsight.testing.TestWebServer;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URL;
import java.util.UUID;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Rajiv Shivane
 */
public class OkHttp3InstrumentationTest {


  private static final boolean DEBUG = false;
  private TestWebServer server;
  private CountTracker tracker;
  private OkHttpClient client;

  @Before
  public void setUp() throws Exception {
    server = new TestWebServer();
    client = new OkHttpClient();
    tracker = new CountTracker(RegistryService.getMetricRegistry(),
        OkHttp3RuleHelper.ROOT_NAME, "method", "status");

    tracker.registerTimer("GET", "200");
    tracker.registerTimer("GET", "301");
    tracker.registerTimer("GET", "302");
    tracker.registerTimer("GET", "400");
    tracker.registerTimer("GET", "404");
    tracker.registerTimer("GET", "500");

    tracker.registerTimer("POST", "200");
    tracker.registerTimer("POST", "400");
    tracker.registerTimer("POST", "404");
    tracker.registerTimer("POST", "500");
  }

  @After
  public void tearDown() throws Exception {
    server.stop();
  }

  @Test
  public void testHttpGet200() throws Exception {
    Snapshot expectedCounts = tracker.snapshot();
    URL url = new URL(server.getEchoEndpoint());
    tracker.validate(expectedCounts); //validate no metric change here

    expectedCounts.increment("GET", "200");
    Response response = newCall(newGetRequest(url)).execute();
    int code = response.code();
    assertEquals(200, code);

    debug(response);
    tracker.validate(expectedCounts);
  }


  @Test
  public void testHttpGet302Redirect() throws Exception {
    Snapshot expectedCounts = tracker.snapshot();
//    expectedCounts.increment("GET", "302");
    expectedCounts.increment("GET", "200");

    URL url = new URL(server.getEchoEndpoint(302) + "&rd=" + server.getEchoEndpoint());
    Response response = newCall(newGetRequest(url)).execute();
    int code = response.code();
    assertEquals(200, code);

    debug(response);
    tracker.validate(expectedCounts);
  }

  @Test
  public void testHttpGet404() throws Exception {
    Snapshot expectedCounts = tracker.snapshot();
    expectedCounts.increment("GET", "404");

    URL url = new URL(server.getEchoEndpoint(404));
    Response response = newCall(newGetRequest(url)).execute();
    int code = response.code();
    assertEquals(404, code);

    debug(response);
    tracker.validate(expectedCounts);
  }


  @Test
  public void testHttpGet500() throws Exception {
    Snapshot expectedCounts = tracker.snapshot();
    expectedCounts.increment("GET", "500");

    URL url = new URL(server.getEchoEndpoint(500));
    Response response = newCall(newGetRequest(url)).execute();
    int code = response.code();
    assertEquals(500, code);

    debug(response);
    tracker.validate(expectedCounts);
  }


  @Test
  public void testConnectErrors() throws Exception {
    Snapshot expectedCounts = tracker.snapshot();

    URL url = new URL("http://0.0.0.0/asd");
    boolean gotConnectError = false;
    Response response = null;
    try {
      response = newCall(newGetRequest(url)).execute();
      int code = response.code();
    } catch (ConnectException e) {
      gotConnectError = true;
    }
    assertTrue(gotConnectError);
    tracker.validate(expectedCounts);//validate no metrics change
  }


  @Test
  public void testPost200() throws Exception {
    Snapshot expectedCounts = tracker.snapshot();
    expectedCounts.increment("POST", "200");

    URL url = new URL(server.getEchoEndpoint());
    String content = UUID.randomUUID().toString();
    Response response = newCall(newPostRequest(url, content)).execute();
    int code = response.code();
    assertEquals(200, code);

    assertThat(response.body().string(), CoreMatchers.containsString(content));

    tracker.validate(expectedCounts);
  }


  @Test
  public void testPost500() throws Exception {
    Snapshot expectedCounts = tracker.snapshot();
    expectedCounts.increment("POST", "500");

    URL url = new URL(server.getEchoEndpoint(500));
    String content = UUID.randomUUID().toString();
    Response response = newCall(newPostRequest(url, content)).execute();
    int code = response.code();
    assertEquals(500, code);

    tracker.validate(expectedCounts);
  }

  @Test
  public void testHttps() throws Exception {
    Snapshot expectedCounts = tracker.snapshot();
    expectedCounts.increment("GET", "200");

    URL url = new URL("https://www.google.com/humans.txt");
    Response response = newCall(newGetRequest(url)).execute();
    int code = response.code();
    assertEquals(200, code);

    debug(response);
    tracker.validate(expectedCounts);
  }


  @Test
  public void testHttps301() throws Exception {
    Snapshot expectedCounts = tracker.snapshot();
//    expectedCounts.increment("GET", "301");
    expectedCounts.increment("GET", "200");

    URL url = new URL("https://google.com/humans.txt");
    Response response = newCall(newGetRequest(url)).execute();
    int code = response.code();
    assertEquals(200, code);

//    debug(response);
    tracker.validate(expectedCounts);
  }

  private Request newGetRequest(URL url) {
    return newGetRequest(url.toString());
  }

  private Request newGetRequest(String url) {
    return new Request.Builder()
        .url(url)
        .build();
  }

  private Request newPostRequest(URL url, String body) {
    return newPostRequest(url.toString(), body);
  }

  private Request newPostRequest(String url, String body) {
    return new Request.Builder()
        .url(url)
        .post(RequestBody.create(MediaType.parse("application/octet-stream"), body))
        .build();
  }

  private Call newCall(Request request) {
    return client.newCall(request);
  }

  private void debug(Response response) throws IOException {
    if (!DEBUG) {
      return;
    }
    System.out.println("Status: " + response.code());
    System.out.println(response.body().string());
  }

}
