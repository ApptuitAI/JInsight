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

package ai.apptuit.metrics.jinsight.modules.httpclient;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import ai.apptuit.metrics.jinsight.RegistryService;
import ai.apptuit.metrics.jinsight.testing.CountTracker;
import ai.apptuit.metrics.jinsight.testing.CountTracker.Snapshot;
import ai.apptuit.metrics.jinsight.testing.TestWebServer;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.util.Scanner;
import java.util.UUID;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Rajiv Shivane
 */
public abstract class AbstractHttpClientInstrumentationTest {

  private static final boolean DEBUG = false;

  private TestWebServer server;
  private HttpClient httpclient;
  private CountTracker tracker;

  @Before
  public void setUp() throws Exception {
    server = new TestWebServer();
    tracker = new CountTracker(RegistryService.getMetricRegistry(),
        HttpClientRuleHelper.ROOT_NAME, "method", "status");
    httpclient = createClient();

    tracker.registerTimer("GET", "200");
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
    HttpGet request = new HttpGet(server.getEchoEndpoint());
    tracker.validate(expectedCounts); //validate no metric change here

    expectedCounts.increment("GET", "200");
    HttpResponse response = httpclient.execute(request);
    tracker.validate(expectedCounts);
    debug(response);
  }


  @Test
  public void testHttpGet302Redirect() throws Exception {
    Snapshot expectedCounts = tracker.snapshot();
    expectedCounts.increment("GET", "302");
    if (clientFollowsRedirects()) {
      expectedCounts.increment("GET", "200");
    }
    HttpGet request = new HttpGet(server.getEchoEndpoint(302) + "&rd=" + server.getEchoEndpoint());

    HttpResponse response = httpclient.execute(request);
    String body = new Scanner(response.getEntity().getContent()).useDelimiter("\0").next();
    tracker.validate(expectedCounts);
    debug(response);
  }

  @Test
  public void testHttpGet404() throws Exception {
    Snapshot expectedCounts = tracker.snapshot();
    expectedCounts.increment("GET", "404");
    HttpGet request = new HttpGet(server.getEchoEndpoint(404));
    HttpResponse response = httpclient.execute(request);
    debug(response);
    tracker.validate(expectedCounts);
  }


  @Test
  public void testHttpGet500() throws Exception {
    Snapshot expectedCounts = tracker.snapshot();
    expectedCounts.increment("GET", "500");
    HttpGet request = new HttpGet(server.getEchoEndpoint(500));
    HttpResponse response = httpclient.execute(request);
    debug(response);
    tracker.validate(expectedCounts);
  }


  @Test
  public void testConnectErrors() throws Exception {
    Snapshot expectedCounts = tracker.snapshot();
    HttpGet request = new HttpGet("http://0.0.0.0/asd");
    HttpResponse response = null;
    try {
      response = httpclient.execute(request);
    } catch (ConnectException e) {
      //expected ... hence assertNull(response)
    }
    assertNull(response);
    tracker.validate(expectedCounts);//validate no metrics change
  }


  @Test
  public void testPost200() throws Exception {
    Snapshot expectedCounts = tracker.snapshot();
    expectedCounts.increment("POST", "200");
    HttpPost request = new HttpPost(server.getEchoEndpoint());
    String content = UUID.randomUUID().toString();
    request.setEntity(new StringEntity(content));
    HttpResponse response = httpclient.execute(request);
    String body = new Scanner(response.getEntity().getContent()).useDelimiter("\0").next();
    assertThat(body, CoreMatchers.containsString(content));
    tracker.validate(expectedCounts);//validate no metrics change
  }


  @Test
  public void testPost500() throws Exception {
    Snapshot expectedCounts = tracker.snapshot();
    expectedCounts.increment("POST", "500");
    HttpPost request = new HttpPost(server.getEchoEndpoint(500));
    String content = UUID.randomUUID().toString();
    request.setEntity(new StringEntity(content));
    HttpResponse response = httpclient.execute(request);
    String body = new Scanner(response.getEntity().getContent()).useDelimiter("\0").next();
    assertThat(body, CoreMatchers.containsString(content));
    tracker.validate(expectedCounts);//validate no metrics change
  }

  private void debug(HttpResponse response) throws IOException {
    if (!DEBUG) {
      return;
    }
    System.out.println("Status: " + response.getStatusLine());
    InputStream content = response.getEntity().getContent();
    System.out.println(TestWebServer.streamToString(content));
  }

  protected abstract HttpClient createClient();

  protected boolean clientFollowsRedirects() {
    return true;
  }

}
