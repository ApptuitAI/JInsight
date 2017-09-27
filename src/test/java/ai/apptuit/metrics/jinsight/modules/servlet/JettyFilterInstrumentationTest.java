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
import ai.apptuit.metrics.jinsight.RegistryService;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


/**
 * @author Rajiv Shivane
 */
public class JettyFilterInstrumentationTest {

  private int serverPort;

  private Server jetty;

  private MetricRegistry registry;

  @Before
  public void setup() throws Exception {
    registry = RegistryService.getMetricRegistry();

    System.out.println("Jetty [Configuring]");

    ServletContextHandler servletContext = new ServletContextHandler();
    servletContext.setContextPath("/");
    servletContext.addServlet(PingPongServlet.class, PingPongServlet.PATH);

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
    TagEncodedMetricName metricName = TagEncodedMetricName.decode(JETTY_METRIC_PREFIX)
        .submetric("requests").withTags("context", ROOT_CONTEXT_PATH, "method", "GET");
    long expectedCount = getTimerCount(metricName) + 1;

    URL url = pathToURL(PingPongServlet.PATH);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.connect();

    assertEquals(HttpServletResponse.SC_OK, connection.getResponseCode());
    assertEquals(PingPongServlet.PONG,
        new Scanner(connection.getInputStream()).useDelimiter("\0").next());

    assertEquals(expectedCount, getTimerCount(metricName));
  }

  private URL pathToURL(String path) throws MalformedURLException {
    return new URL("http://localhost:" + serverPort + path);
  }


  private long getTimerCount(TagEncodedMetricName name) {
    Timer timer = registry.getTimers().get(name.toString());
    return timer != null ? timer.getCount() : 0;
  }

  public static class PingPongServlet extends HttpServlet {

    public static final String PONG = "pong";
    public static final String PATH = "/ping";

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
      response.setStatus(HttpServletResponse.SC_OK);
      response.getWriter().append(PONG);
    }
  }
}
