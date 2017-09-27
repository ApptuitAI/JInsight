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

package ai.apptuit.metrics.jinsight.modules.common;

import ai.apptuit.metrics.dropwizard.TagEncodedMetricName;
import ai.apptuit.metrics.jinsight.RegistryService;
import com.codahale.metrics.Timer.Context;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.WeakHashMap;
import org.jboss.byteman.rule.Rule;
import org.jboss.byteman.rule.helper.Helper;

/**
 * @author Rajiv Shivane
 */
public class RuleHelper extends Helper {

  private static final Map<Object, Map<String, String>> objectProperties = Collections
      .synchronizedMap(new WeakHashMap<Object, Map<String, String>>());

  public RuleHelper(Rule rule) {
    super(rule);
  }

  public void startTimer(TagEncodedMetricName metric) {
    Timers.start(metric);
  }

  public void stopTimer(TagEncodedMetricName metric) {
    Timers.stop(metric);
  }

  public String setObjectProperty(Object o, String propertyName, String propertyValue) {
    Map<String, String> props = objectProperties
        .computeIfAbsent(o, k -> Collections.synchronizedMap(new HashMap<>()));
    return props.put(propertyName, propertyValue);
  }

  public String getObjectProperty(Object o, String propertyName) {
    Map<String, String> props = objectProperties.get(o);
    if (props == null) {
      return null;
    }
    return props.get(propertyName);
  }

  private static class Timers {

    private static final ThreadLocal<Stack<Context>> TIMERS = ThreadLocal.withInitial(Stack::new);

    public static void start(TagEncodedMetricName metric) {
      TIMERS.get().push(RegistryService.getMetricRegistry().timer(metric.toString()).time());
    }

    public static void stop(TagEncodedMetricName metric) {
      Context context = TIMERS.get().pop();
      long t = context.stop();
      //TODO verify the Context we popped is the one for the same metric
      //System.out.printf("Done in [%d] nanos\n", t);
    }
  }
}
