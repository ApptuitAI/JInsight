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

import ai.apptuit.metrics.dropwizard.TagEncodedMetricName;
import ai.apptuit.metrics.jinsight.modules.common.RuleHelper;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.component.AbstractLifeCycle.AbstractLifeCycleListener;
import org.eclipse.jetty.util.component.LifeCycle;
import org.jboss.byteman.rule.Rule;

/**
 * @author Rajiv Shivane
 */
public class JettyRuleHelper extends RuleHelper {

  public static final TagEncodedMetricName JETTY_METRIC_PREFIX = TagEncodedMetricName
      .decode("jetty");

  public JettyRuleHelper(Rule rule) {
    super(rule);
  }

  public void instrument(Server server) {
    server.addLifeCycleListener(new AbstractLifeCycleListener() {
      @Override
      public void lifeCycleStarting(LifeCycle event) {
        server.insertHandler(new JettyMetricsHandler());
      }
    });
  }

  private static class JettyMetricsHandler extends HandlerWrapper {

    private Map<String, ContextMetricsHelper> helpers = new ConcurrentHashMap<>();

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request,
        HttpServletResponse response) throws IOException, ServletException {

      String contextPath = extractContextPath(request);
      ContextMetricsHelper helper = getMetricsHelper(contextPath);

      helper.measure(request, response, () -> {
        super.handle(target, baseRequest, request, response);
      });

    }

    private String extractContextPath(HttpServletRequest request) {
      Handler[] handlers = this.getHandlers();
      String contextPath = request.getContextPath();
      for (Handler handler : handlers) {
        if (handler instanceof ServletContextHandler) {
          contextPath = ((ServletContextHandler) handler).getContextPath();
          break;
        }
      }

      if (contextPath == null || contextPath.length() == 1) {
        contextPath = "";
      }
      return contextPath;
    }

    private ContextMetricsHelper getMetricsHelper(String contextPath) {
      return helpers.computeIfAbsent(contextPath, s -> {
        return new ContextMetricsHelper(JETTY_METRIC_PREFIX, contextPath);
      });
    }
  }
}
