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

import ai.apptuit.metrics.dropwizard.TagEncodedMetricName;
import ai.apptuit.metrics.jinsight.RegistryService;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import java.io.IOException;
import java.util.EnumSet;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.FilterRegistration.Dynamic;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.jboss.byteman.rule.Rule;

/**
 * @author Rajiv Shivane
 */
public class ServletRuleHelper extends RuleHelper {

  public static final String TOMCAT_METRIC_PREFIX = "tomcat";
  public static final String JETTY_METRIC_PREFIX = "jetty";
  public static final String ROOT_CONTEXT_PATH = "ROOT";
  private static final String PROPERTY_VALUE_TRUE = "TRUE";
  private static final String PROPERTY_NAME_IS_FILTER_ADDED = "isFilterAdded";

  public ServletRuleHelper(Rule rule) {
    super(rule);
  }


  public void instrumentServletContextOnce(ServletContext context) {
    if (PROPERTY_VALUE_TRUE.equals(getObjectProperty(context, PROPERTY_NAME_IS_FILTER_ADDED))) {
      return;
    }

    MetricsFilter filter = new MetricsFilter();
    Dynamic dynamic = context.addFilter(filter.getClass().getName(), filter);
    dynamic.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");

    setObjectProperty(context, PROPERTY_NAME_IS_FILTER_ADDED, PROPERTY_VALUE_TRUE);
  }

  private static class MetricsFilter implements Filter {

    private TagEncodedMetricName rootMetric;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
      ServletContext context = filterConfig.getServletContext();
      String contextPathTag = context.getContextPath();
      if (contextPathTag.trim().equals("")) {
        contextPathTag = ROOT_CONTEXT_PATH;
      }
      rootMetric = TagEncodedMetricName.decode(getServerName(context.getServerInfo()))
          .submetric("requests").withTags("context", contextPathTag);
    }

    private String getServerName(String serverInfo) {

      String serverName;
      int slash = serverInfo.indexOf('/');
      if (slash == -1) {
        serverName = serverInfo;
      } else {
        serverName = serverInfo.substring(0, slash);
      }

      if (serverName.toLowerCase().contains("tomcat")) {
        return TOMCAT_METRIC_PREFIX;
      } else if (serverName.toLowerCase().contains("jetty")) {
        return JETTY_METRIC_PREFIX;
      } else {
        return serverName;
      }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
        FilterChain filterChain) throws IOException, ServletException {

      TagEncodedMetricName metricName = rootMetric;
      if (servletRequest instanceof HttpServletRequest) {
        metricName = metricName
            .withTags("method", ((HttpServletRequest) servletRequest).getMethod());
      }
      Timer timer = RegistryService.getMetricRegistry().timer(metricName.toString());
      Context context = timer.time();
      try {
        filterChain.doFilter(servletRequest, servletResponse);
      } finally {
        context.stop();
      }
    }

    @Override
    public void destroy() {
    }
  }
}
