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

import ai.apptuit.metrics.jinsight.RegistryService;
import ai.apptuit.metrics.jinsight.testing.CountTracker;
import ai.apptuit.metrics.jinsight.testing.CountTracker.Snapshot;
import ai.apptuit.metrics.jinsight.testing.TestWebServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.util.Scanner;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.impl.DefaultHttpClientConnection;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.apache.http.protocol.RequestContent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Rajiv Shivane
 */
@SuppressWarnings("deprecated")
public class RequestExecutorBasedClientInstrumentationTest {

  private TestWebServer server;
  private HTTPClient httpclient;
  private CountTracker tracker;

  @Before
  public void setUp() throws Exception {
    server = new TestWebServer();
    tracker = new CountTracker(RegistryService.getMetricRegistry(),
        HttpClientRuleHelper.ROOT_NAME, "method", "status");
    httpclient = new HTTPClient(server.getAddress());

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
  public void testGet200() throws Exception {
    Snapshot expectedCounts = tracker.snapshot();
    expectedCounts.increment("GET", "200");

    HttpResponse response = httpclient.doGET(new URI(server.getEchoEndpoint()));
    System.out.println(new Scanner(response.getEntity().getContent()).useDelimiter("\0").next());
    tracker.validate(expectedCounts);
  }


  @Test
  public void testGet500() throws Exception {
    Snapshot expectedCounts = tracker.snapshot();
    expectedCounts.increment("GET", "500");

    HttpResponse response = httpclient.doGET(new URI(server.getEchoEndpoint(500)));
    System.out.println(new Scanner(response.getEntity().getContent()).useDelimiter("\0").next());
    tracker.validate(expectedCounts);
  }

  public class HTTPClient {

    public static final String DEFAULT_USER_AGENT = "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1)";

    final private InetSocketAddress address;
    final private DefaultHttpClientConnection connection = new DefaultHttpClientConnection();

    public HTTPClient(InetSocketAddress address) {
      this.address = address;
    }

    public void close() {
      try {
        connection.close();
      } catch (IOException e) {
      }
    }

    public HttpResponse execute(HttpRequest request) throws IOException, HttpException {
      HttpParams params = new BasicHttpParams();
      HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);

      HttpProcessor processor = new ImmutableHttpProcessor(new RequestContent());

      HttpRequestExecutor executor = new HttpRequestExecutor();

      HttpContext context = new BasicHttpContext(null);
      context.setAttribute(ExecutionContext.HTTP_CONNECTION, connection);

      if (!connection.isOpen()) {
        Socket socket = new Socket(address.getAddress(), address.getPort());
        connection.bind(socket, params);
      }

      context.setAttribute(ExecutionContext.HTTP_REQUEST, request);
      request.setParams(params);
      executor.preProcess(request, processor, context);
      HttpResponse response = executor.execute(request, connection, context);
      executor.postProcess(response, processor, context);

      return response;
    }

    public HttpResponse doGET(URI url) throws IOException, HttpException {
      String uri = url.getRawPath() + (url.getRawQuery() != null ? "?" + url.getRawQuery() : "");
      HttpRequest request = new BasicHttpRequest("GET", uri);
      String hostHeader = (url.getPort() == 0 || url.getPort() == 80)
          ? url.getHost() : (url.getHost() + ":" + url.getPort());
      request.addHeader("Host", hostHeader);
      return execute(request);
    }
  }
}
