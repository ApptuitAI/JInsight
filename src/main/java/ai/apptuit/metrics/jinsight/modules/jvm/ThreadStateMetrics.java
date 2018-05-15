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

package ai.apptuit.metrics.jinsight.modules.jvm;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.jvm.CachedThreadStatesGaugeSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Rajiv Shivane
 */
class ThreadStateMetrics implements MetricSet {

  private final Map<String, Metric> metrics = new HashMap<>();

  public ThreadStateMetrics() {
    MetricSet delegate = new CachedThreadStatesGaugeSet(60, TimeUnit.SECONDS);
    delegate.getMetrics().forEach((key, value) -> {
      metrics.put(getMetricName(key), value);
    });
  }

  @Override
  public Map<String, Metric> getMetrics() {
    return Collections.unmodifiableMap(metrics);
  }

  private String getMetricName(String key) throws IllegalArgumentException {
    switch (key) {
      case "count":
        return "total.count";
      case "blocked.count":
        return "count[state:blocked]";
      case "new.count":
        return "count[state:new]";
      case "runnable.count":
        return "count[state:runnable]";
      case "terminated.count":
        return "count[state:terminated]";
      case "timed_waiting.count":
        return "count[state:timed_waiting]";
      case "waiting.count":
        return "count[state:waiting]";
      default:
        return key;
    }
  }

}
