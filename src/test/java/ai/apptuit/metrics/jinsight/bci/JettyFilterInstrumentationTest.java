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

package ai.apptuit.metrics.jinsight.bci;

import static ai.apptuit.metrics.jinsight.bci.ServletRuleHelper.JETTY_METRIC_PREFIX;
import static ai.apptuit.metrics.jinsight.bci.ServletRuleHelper.ROOT_CONTEXT_PATH;
import static org.junit.Assert.assertEquals;

import ai.apptuit.metrics.dropwizard.TagEncodedMetricName;
import ai.apptuit.metrics.jinsight.RegistryService;
import ai.apptuit.metrics.util.MockMetricsRegistry;
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
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.testng.PowerMockTestCase;


/**
 * @author Rajiv Shivane
 */
@PrepareForTest({RegistryService.class})
@PowerMockIgnore({"org.jboss.byteman.*", "javax.net.ssl.*"})
@RunWith(PowerMockRunner.class)
public class JettyFilterInstrumentationTest extends PowerMockTestCase {

  private int serverPort;

  private Server jetty;

  private MockMetricsRegistry metricsRegistry;

  @Before
  public void setup() throws Exception {
    metricsRegistry = MockMetricsRegistry.getInstance();

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
    TagEncodedMetricName metricName = TagEncodedMetricName.decode(JETTY_METRIC_PREFIX).submetric("requests")
        .withTags("context", ROOT_CONTEXT_PATH);
    int expectStartCount = metricsRegistry.getStartCount(metricName) + 1;
    int expectedStopCount = metricsRegistry.getStopCount(metricName) + 1;

    URL url = pathToURL(PingPongServlet.PATH);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.connect();

    assertEquals(HttpServletResponse.SC_OK, connection.getResponseCode());
    assertEquals(PingPongServlet.PONG,
        new Scanner(connection.getInputStream()).useDelimiter("\0").next());

    assertEquals(expectStartCount, metricsRegistry.getStartCount(metricName));
    assertEquals(expectedStopCount, metricsRegistry.getStopCount(metricName));
  }

  private URL pathToURL(String path) throws MalformedURLException {
    return new URL("http://localhost:" + serverPort + path);
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
