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

import ai.apptuit.metrics.jinsight.modules.common.RuleHelper;
import java.util.EnumSet;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration.Dynamic;
import javax.servlet.ServletContext;
import org.jboss.byteman.rule.Rule;

/**
 * @author Rajiv Shivane
 */
public class ServletRuleHelper extends RuleHelper {

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
    dynamic.setAsyncSupported(true);
    dynamic.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");

    setObjectProperty(context, PROPERTY_NAME_IS_FILTER_ADDED, PROPERTY_VALUE_TRUE);
  }
}
