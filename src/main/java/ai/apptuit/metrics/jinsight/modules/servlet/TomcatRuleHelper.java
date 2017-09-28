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
import org.apache.catalina.Lifecycle;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.valves.ValveBase;
import org.jboss.byteman.rule.Rule;

/**
 * @author Rajiv Shivane
 */
public class TomcatRuleHelper extends ServletRuleHelper {

  public static final String TOMCAT_METRIC_PREFIX = "tomcat";

  public TomcatRuleHelper(Rule rule) {
    super(rule);
  }


  public void instrumentPipeline(StandardContext standardContext) {
    standardContext.addLifecycleListener(event -> {
      String contextPath = standardContext.getPath();
      if (Lifecycle.BEFORE_INIT_EVENT.equals(event.getType())) {
        MyValve valve = new MyValve();
        standardContext.addValve(valve);
        valve.init(contextPath);
      }
    });
  }

  private static class MyValve extends ValveBase {


    private MetricRegistry registry;
    private TagEncodedMetricName requestCountRootMetric;
    private TagEncodedMetricName responseCountRootMetric;

    private Counter activeRequestsCounter;
    private Map<String, Timer> timersByMethod = new ConcurrentHashMap<>();
    private Map<Integer, Meter> metersByStatusCode = new ConcurrentHashMap<>();

    public MyValve() {
      super(true);
    }

    public void init(String contextPath) {
      if (contextPath.trim().equals("")) {
        contextPath = ServletRuleHelper.ROOT_CONTEXT_PATH;
      }
      TagEncodedMetricName serverRootMetric = TagEncodedMetricName.decode(TOMCAT_METRIC_PREFIX)
          .withTags("context", contextPath);
      requestCountRootMetric = serverRootMetric.submetric("requests");
      responseCountRootMetric = serverRootMetric.submetric("responses");
      registry = RegistryService.getMetricRegistry();
      activeRequestsCounter = registry
          .counter(serverRootMetric.submetric("requests.active").toString());
    }


    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {

      activeRequestsCounter.inc();
      Context context = getTimerByMethod(request).time();

      this.getNext().invoke(request, response);

      if (request.isAsync() && request.isAsyncStarted()) {
        request.getAsyncContext().addListener(new AsyncCompletionListener(context, response));
      } else {
        context.stop();
        activeRequestsCounter.dec();
        updateStatusMetric(response);
      }
    }

    private void updateStatusMetric(Response response) {
      getMeterByStatus(response.getStatus()).mark();
    }

    private Timer getTimerByMethod(Request servletRequest) {
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

    private class AsyncCompletionListener implements AsyncListener {

      private final Context context;
      private final Response response;
      private boolean done;

      public AsyncCompletionListener(Context context, Response response) {
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
}
