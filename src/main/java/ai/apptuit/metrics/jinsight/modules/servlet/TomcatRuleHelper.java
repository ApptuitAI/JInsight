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

import ai.apptuit.metrics.client.TagEncodedMetricName;
import ai.apptuit.metrics.jinsight.modules.common.RuleHelper;
import java.io.IOException;
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
public class TomcatRuleHelper extends RuleHelper {

  public static final TagEncodedMetricName TOMCAT_METRIC_PREFIX = TagEncodedMetricName
      .decode("tomcat");

  public TomcatRuleHelper(Rule rule) {
    super(rule);
  }


  public void instrument(StandardContext standardContext) {
    standardContext.addLifecycleListener(event -> {
      String contextPath = standardContext.getPath();
      if (Lifecycle.BEFORE_INIT_EVENT.equals(event.getType())) {
        TomcatMetricsValve valve = new TomcatMetricsValve(contextPath);
        standardContext.addValve(valve);
      }
    });
  }

  private static class TomcatMetricsValve extends ValveBase {

    private final ContextMetricsHelper helper;

    public TomcatMetricsValve(String contextPath) {
      super(true);

      helper = new ContextMetricsHelper(TOMCAT_METRIC_PREFIX, contextPath);
    }


    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {

      helper.measure(request, response, () -> {
        this.getNext().invoke(request, response);
      });
    }
  }
}
