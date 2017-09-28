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
import ai.apptuit.metrics.jinsight.RegistryService;
import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

/**
 * @author Rajiv Shivane
 */
class MetricsFilter implements Filter {

  private MetricRegistry registry;
  private TagEncodedMetricName requestCountRootMetric;

  private Counter activeRequestsCounter;
  private Map<String, Timer> timersByMethod = new ConcurrentHashMap<>();

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    ServletContext context = filterConfig.getServletContext();
    String contextPathTag = context.getContextPath();
    if (contextPathTag.trim().equals("")) {
      contextPathTag = ServletRuleHelper.ROOT_CONTEXT_PATH;
    }
    TagEncodedMetricName serverRootMetric = TagEncodedMetricName
        .decode(getServerName(context.getServerInfo())).withTags("context", contextPathTag);
    requestCountRootMetric = serverRootMetric.submetric("requests");
    registry = RegistryService.getMetricRegistry();
    activeRequestsCounter = registry
        .counter(serverRootMetric.submetric("requests.active").toString());
  }

  private String getServerName(String serverInfo) {

    String serverName;
    int slash = serverInfo.indexOf('/');
    if (slash == -1) {
      serverName = serverInfo;
    } else {
      serverName = serverInfo.substring(0, slash);
    }

    if (serverName.toLowerCase().contains("jetty")) {
      return ServletRuleHelper.JETTY_METRIC_PREFIX;
    } else {
      return serverName;
    }
  }

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
      FilterChain filterChain) throws IOException, ServletException {

    activeRequestsCounter.inc();
    Context context = getTimerByMethod(servletRequest).time();

    boolean cleanRun = false;
    try {
      filterChain.doFilter(servletRequest, servletResponse);
      cleanRun = true;
    } finally {
      if (cleanRun && servletRequest.isAsyncStarted()) {
        servletRequest.getAsyncContext().addListener(new AsyncCompletionListener(context));
      } else {
        context.stop();
        activeRequestsCounter.dec();
      }
    }
  }

  private Timer getTimerByMethod(ServletRequest servletRequest) {
    String method = null;
    if (servletRequest instanceof HttpServletRequest) {
      method = ((HttpServletRequest) servletRequest).getMethod();
    }
    return getTimerByMethod(method);
  }

  private Timer getTimerByMethod(String method) {
    String key = (method != null) ? method : "?";
    return timersByMethod.computeIfAbsent(key, s -> {
      TagEncodedMetricName metric = requestCountRootMetric;
      if (method != null) {
        metric = metric.withTags("method", method);
      }
      return registry.timer(metric.toString());
    });
  }

  @Override
  public void destroy() {
  }

  private class AsyncCompletionListener implements AsyncListener {

    private Context context;
    private boolean processed = false;

    public AsyncCompletionListener(Context context) {
      this.context = context;
    }

    @Override
    public void onComplete(AsyncEvent event) throws IOException {
      if (processed) {
        return;
      }
      context.stop();
      activeRequestsCounter.dec();
    }

    @Override
    public void onTimeout(AsyncEvent event) throws IOException {
      context.stop();
      activeRequestsCounter.dec();
      processed = true;
    }

    @Override
    public void onError(AsyncEvent event) throws IOException {
      context.stop();
      activeRequestsCounter.dec();
      processed = true;
    }

    @Override
    public void onStartAsync(AsyncEvent event) throws IOException {
    }
  }
}
