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

package ai.apptuit.metrics.jinsight.modules.httpurlconnection;

import static org.junit.Assert.assertThat;

import ai.apptuit.metrics.jinsight.RegistryService;
import ai.apptuit.metrics.jinsight.testing.CountTracker;
import ai.apptuit.metrics.jinsight.testing.CountTracker.Snapshot;
import ai.apptuit.metrics.jinsight.testing.TestWebServer;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.util.UUID;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Rajiv Shivane
 */
public class UrlConnectionInstrumentationTest {


  private static final boolean DEBUG = false;
  private TestWebServer server;
  private CountTracker tracker;

  @Before
  public void setUp() throws Exception {
    server = new TestWebServer();
    tracker = new CountTracker(RegistryService.getMetricRegistry(),
        UrlConnectionRuleHelper.ROOT_NAME, "method", "status");

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
    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
    int code = urlConnection.getResponseCode();

    debug(urlConnection);
    tracker.validate(expectedCounts);
  }


  @Test
  public void testHttpGet302Redirect() throws Exception {
    Snapshot expectedCounts = tracker.snapshot();
    expectedCounts.increment("GET", "302");
    expectedCounts.increment("GET", "200");

    URL url = new URL(server.getEchoEndpoint(302) + "&rd=" + server.getEchoEndpoint());
    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
    int code = urlConnection.getResponseCode();

    debug(urlConnection);
    tracker.validate(expectedCounts);
  }

  @Test
  public void testHttpGet404() throws Exception {
    Snapshot expectedCounts = tracker.snapshot();
    expectedCounts.increment("GET", "404");

    URL url = new URL(server.getEchoEndpoint(404));
    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
    try {
      int code = urlConnection.getResponseCode();
    } catch (FileNotFoundException e) {
      //expected
    }

    tracker.validate(expectedCounts);
  }


  @Test
  public void testHttpGet500() throws Exception {
    Snapshot expectedCounts = tracker.snapshot();
    expectedCounts.increment("GET", "500");

    URL url = new URL(server.getEchoEndpoint(500));
    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
    try {
      int code = urlConnection.getResponseCode();
    } catch (IOException e) {
      //expected
    }

    tracker.validate(expectedCounts);
  }


  @Test
  public void testConnectErrors() throws Exception {
    Snapshot expectedCounts = tracker.snapshot();

    URL url = new URL("http://0.0.0.0/asd");
    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
    try {
      int code = urlConnection.getResponseCode();
    } catch (ConnectException e) {
      //expected
    }

    tracker.validate(expectedCounts);//validate no metrics change
  }


  @Test
  public void testPost200() throws Exception {
    Snapshot expectedCounts = tracker.snapshot();
    expectedCounts.increment("POST", "200");

    URL url = new URL(server.getEchoEndpoint());
    String content = UUID.randomUUID().toString();
    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

    urlConnection.setDoOutput(true);
    OutputStream outputStream = urlConnection.getOutputStream();
    outputStream.write(content.getBytes());
    outputStream.flush();

    int code = urlConnection.getResponseCode();
    String body = new Scanner(urlConnection.getInputStream()).useDelimiter("\0").next();
    assertThat(body, CoreMatchers.containsString(content));

    debug(urlConnection);
    tracker.validate(expectedCounts);
  }


  @Test
  public void testPost500() throws Exception {
    Snapshot expectedCounts = tracker.snapshot();
    expectedCounts.increment("POST", "500");

    URL url = new URL(server.getEchoEndpoint(500));
    String content = UUID.randomUUID().toString();
    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

    urlConnection.setDoOutput(true);
    OutputStream outputStream = urlConnection.getOutputStream();
    outputStream.write(content.getBytes());
    outputStream.flush();

    try {
      int code = urlConnection.getResponseCode();
    } catch (IOException e) {
      //expected
    }

    tracker.validate(expectedCounts);
  }

  @Test
  public void testHttps() throws Exception {
    Snapshot expectedCounts = tracker.snapshot();
    expectedCounts.increment("GET", "200");

    URL url = new URL("https://www.google.com/humans.txt");
    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
    int code = urlConnection.getResponseCode();

    debug(urlConnection);
    tracker.validate(expectedCounts);
  }

  @Test
  public void testHttps301() throws Exception {
    Snapshot expectedCounts = tracker.snapshot();
    expectedCounts.increment("GET", "301");
    expectedCounts.increment("GET", "200");

    URL url = new URL("https://google.com/humans.txt");
    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
    int code = urlConnection.getResponseCode();

    debug(urlConnection);
    tracker.validate(expectedCounts);
  }

  private void debug(HttpURLConnection connection) throws IOException {
    if (!DEBUG) {
      return;
    }
    System.out.println("Status: " + connection.getResponseCode());
    InputStream content = connection.getInputStream();
    System.out.println(TestWebServer.streamToString(content));
  }

}
