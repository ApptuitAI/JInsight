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

import static ai.apptuit.metrics.jinsight.modules.servlet.ContextMetricsHelper.ROOT_CONTEXT_PATH;
import static ai.apptuit.metrics.jinsight.modules.servlet.JettyRuleHelper.JETTY_METRIC_PREFIX;

import ai.apptuit.metrics.dropwizard.TagEncodedMetricName;
import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.After;
import org.junit.Before;

/**
 * @author Rajiv Shivane
 */
public class JettyFilterInstrumentationTest extends AbstractWebServerTest {

  private int serverPort;
  private Server jetty;

  @Before
  public void setup() throws Exception {
    super.setup();

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

  @Override
  protected TagEncodedMetricName getRootMetric() {
    return JETTY_METRIC_PREFIX.withTags("context", ROOT_CONTEXT_PATH);
  }

  protected URL pathToURL(String path) throws MalformedURLException {
    return new URL("http://localhost:" + serverPort + path);
  }
}
