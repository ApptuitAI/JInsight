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

package ai.apptuit.metrics.jinsight.modules.servlet;

import static ai.apptuit.metrics.jinsight.modules.servlet.ServletRuleHelper.JETTY_METRIC_PREFIX;
import static ai.apptuit.metrics.jinsight.modules.servlet.ServletRuleHelper.ROOT_CONTEXT_PATH;
import static org.junit.Assert.assertEquals;

import ai.apptuit.metrics.dropwizard.TagEncodedMetricName;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.UUID;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Rajiv Shivane
 */
public class JettyFilterInstrumentationTest extends AbstractWebServerTest {

  private int serverPort;
  private Server jetty;

  @Before
  public void setup() throws Exception {
    TagEncodedMetricName base = TagEncodedMetricName.decode(JETTY_METRIC_PREFIX)
        .withTags("context", ROOT_CONTEXT_PATH);
    setupMetrics(base.submetric("requests"), base.submetric("responses"));

    System.out.println("Jetty [Configuring]");

    ServletContextHandler servletContext = new ServletContextHandler();
    servletContext.setContextPath("/");
    servletContext.addServlet(PingPongServlet.class, PingPongServlet.PATH);
    servletContext.addServlet(ExceptionServlet.class, ExceptionServlet.PATH);
    ServletHolder servletHolder = servletContext.addServlet(AsyncServlet.class, AsyncServlet.PATH);
    servletHolder.setAsyncSupported(true);

    jetty = new Server(0);
    jetty.setHandler(servletContext);
    System.out.println("Jetty [Starting]");
    jetty.start();
    System.out.println("Jetty [Started]");
    serverPort = ((ServerConnector) jetty.getConnectors()[0]).getLocalPort();
  }

  @After
  public void destroy() throws Exception {
    System.out.println("Jetty [Stopping]");
    jetty.stop();
    jetty.join();
    System.out.println("Jetty [Stopped]");
  }

  @Test
  public void testPingPong() throws IOException {
    Map<String, Long> expectedCounts = getCurrentCounts();
    expectedCounts.compute("GET", (s, aLong) -> aLong + 1);

    URL url = pathToURL(PingPongServlet.PATH);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.connect();

    assertEquals(HttpServletResponse.SC_OK, connection.getResponseCode());
    assertEquals(PingPongServlet.PONG, getText(connection));

    assertEquals(expectedCounts, getCurrentCounts());
  }


  @Test
  public void testAsync() throws IOException {
    Map<String, Long> expectedCounts = getCurrentCounts();
    expectedCounts.compute("GET", (s, aLong) -> aLong + 1);

    String uuid = UUID.randomUUID().toString();
    URL url = pathToURL(AsyncServlet.PATH + "?" + AsyncServlet.UUID_PARAM + "=" + uuid);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.connect();

    assertEquals(HttpServletResponse.SC_OK, connection.getResponseCode());
    assertEquals(uuid, getText(connection));

    assertEquals(expectedCounts, getCurrentCounts());
  }


  @Test
  public void testAsyncWithError() throws IOException {
    Map<String, Long> expectedCounts = getCurrentCounts();
    expectedCounts.compute("GET", (s, aLong) -> aLong + 1);

    URL url = pathToURL(AsyncServlet.PATH);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.connect();

    assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, connection.getResponseCode());
    assertEquals(expectedCounts, getCurrentCounts());
  }

  @Test
  public void testPost() throws IOException {
    Map<String, Long> expectedCounts = getCurrentCounts();
    expectedCounts.compute("POST", (s, aLong) -> aLong + 1);

    String content = UUID.randomUUID().toString();

    URL url = pathToURL(PingPongServlet.PATH);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("POST");
    connection.setDoOutput(true);
    connection.getOutputStream().write(content.getBytes());
    connection.connect();

    assertEquals(HttpServletResponse.SC_OK, connection.getResponseCode());
    assertEquals(content, getText(connection));

    assertEquals(expectedCounts, getCurrentCounts());
  }


  @Test
  public void testExceptionResponse() throws IOException {
    Map<String, Long> expectedCounts = getCurrentCounts();
    expectedCounts.compute("GET", (s, aLong) -> aLong + 1);

    URL url = pathToURL(ExceptionServlet.PATH);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("GET");
    connection.connect();

    assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, connection.getResponseCode());
    assertEquals(expectedCounts, getCurrentCounts());
  }

  private URL pathToURL(String path) throws MalformedURLException {
    return new URL("http://localhost:" + serverPort + path);
  }
}
