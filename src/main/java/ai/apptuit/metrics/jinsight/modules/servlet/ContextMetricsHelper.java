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
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Rajiv Shivane
 */
public class ContextMetricsHelper {

  public static final String ROOT_CONTEXT_PATH = "ROOT";

  private MetricRegistry registry;
  private TagEncodedMetricName requestCountRootMetric;

  private Counter activeRequestsCounter;
  private Map<String, Timer> timers = new ConcurrentHashMap<>();

  public ContextMetricsHelper(TagEncodedMetricName serverPrefix, String contextPath) {
    if (contextPath.trim().equals("")) {
      contextPath = ROOT_CONTEXT_PATH;
    }
    TagEncodedMetricName serverRootMetric = serverPrefix.withTags("context", contextPath);
    requestCountRootMetric = serverRootMetric.submetric("requests");
    registry = RegistryService.getMetricRegistry();
    activeRequestsCounter = registry
        .counter(serverRootMetric.submetric("requests.active").toString());
  }


  public void measure(HttpServletRequest request, HttpServletResponse response,
      MeasurableJob runnable)
      throws IOException, ServletException {

    activeRequestsCounter.inc();

    long startTime = System.nanoTime();
    runnable.run();
    if (request.isAsyncStarted()) {
      request.getAsyncContext()
          .addListener(new AsyncCompletionListener(startTime, request, response));
    } else {
      activeRequestsCounter.dec();
      updateStatusMetric(startTime, request.getMethod(), response.getStatus());
    }
  }

  private void updateStatusMetric(long startTime, String method, int status) {
    getTimer(method, status).update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
  }

  private Timer getTimer(String method, int status) {
    String key = String.valueOf(status) + method;
    return timers.computeIfAbsent(key, s -> {
      TagEncodedMetricName metric = requestCountRootMetric;
      if (method != null) {
        metric = metric.withTags("method", method, "status", String.valueOf(status));
      }
      return registry.timer(metric.toString());
    });
  }

  public interface MeasurableJob {

    public void run() throws IOException, ServletException;
  }

  private class AsyncCompletionListener implements AsyncListener {

    private final HttpServletResponse response;
    private long startTime;
    private HttpServletRequest request;
    private boolean done;

    public AsyncCompletionListener(long startTime, HttpServletRequest request,
        HttpServletResponse response) {
      this.startTime = startTime;
      this.request = request;
      this.response = response;
      done = false;
    }

    @Override
    public void onComplete(AsyncEvent event) throws IOException {
      if (done) {
        return;
      }
      activeRequestsCounter.dec();
      updateStatusMetric(startTime, request.getMethod(), response.getStatus());
    }

    @Override
    public void onTimeout(AsyncEvent event) throws IOException {
      activeRequestsCounter.dec();
      updateStatusMetric(startTime, request.getMethod(), response.getStatus());
      done = true;
    }

    @Override
    public void onError(AsyncEvent event) throws IOException {
      activeRequestsCounter.dec();
      updateStatusMetric(startTime, request.getMethod(), response.getStatus());
      done = true;
    }

    @Override
    public void onStartAsync(AsyncEvent event) throws IOException {
    }
  }
}
