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
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
  private TagEncodedMetricName responseCountRootMetric;

  private Counter activeRequestsCounter;
  private Map<String, Timer> timersByMethod = new ConcurrentHashMap<>();
  private Map<Integer, Meter> metersByStatusCode = new ConcurrentHashMap<>();

  public ContextMetricsHelper(TagEncodedMetricName serverPrefix, String contextPath) {
    if (contextPath.trim().equals("")) {
      contextPath = ROOT_CONTEXT_PATH;
    }
    TagEncodedMetricName serverRootMetric = serverPrefix.withTags("context", contextPath);
    requestCountRootMetric = serverRootMetric.submetric("requests");
    responseCountRootMetric = serverRootMetric.submetric("responses");
    registry = RegistryService.getMetricRegistry();
    activeRequestsCounter = registry
        .counter(serverRootMetric.submetric("requests.active").toString());
  }


  public void measure(HttpServletRequest request, HttpServletResponse response,
      MeasurableJob runnable)
      throws IOException, ServletException {

    activeRequestsCounter.inc();
    Context context = getTimerByMethod(request).time();

    runnable.run();
    if (request.isAsyncStarted()) {
      request.getAsyncContext().addListener(new AsyncCompletionListener(context, response));
    } else {
      context.stop();
      activeRequestsCounter.dec();
      updateStatusMetric(response);
    }
  }

  private void updateStatusMetric(HttpServletResponse response) {
    getMeterByStatus(response.getStatus()).mark();
  }

  private Timer getTimerByMethod(HttpServletRequest servletRequest) {
    return getTimerByMethod(servletRequest.getMethod());
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

  private Meter getMeterByStatus(Integer status) {
    return metersByStatusCode.computeIfAbsent(status, s -> {
      TagEncodedMetricName metric = responseCountRootMetric.withTags("status", status.toString());
      return registry.meter(metric.toString());
    });
  }

  public interface MeasurableJob {

    public void run() throws IOException, ServletException;
  }

  private class AsyncCompletionListener implements AsyncListener {

    private final Context context;
    private final HttpServletResponse response;
    private boolean done;

    public AsyncCompletionListener(Context context, HttpServletResponse response) {
      this.context = context;
      this.response = response;
      done = false;
    }

    @Override
    public void onComplete(AsyncEvent event) throws IOException {
      if (done) {
        return;
      }
      context.stop();
      activeRequestsCounter.dec();
      updateStatusMetric(response);
    }

    @Override
    public void onTimeout(AsyncEvent event) throws IOException {
      context.stop();
      activeRequestsCounter.dec();
      updateStatusMetric(response);
      done = true;
    }

    @Override
    public void onError(AsyncEvent event) throws IOException {
      context.stop();
      activeRequestsCounter.dec();
      updateStatusMetric(response);
      done = true;
    }

    @Override
    public void onStartAsync(AsyncEvent event) throws IOException {
    }
  }
}
