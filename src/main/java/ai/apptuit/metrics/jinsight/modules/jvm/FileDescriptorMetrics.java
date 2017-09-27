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

import static java.lang.management.ManagementFactory.getOperatingSystemMXBean;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Rajiv Shivane
 */
class FileDescriptorMetrics implements MetricSet {

  private OperatingSystemMXBean osMxBean;

  public FileDescriptorMetrics() {
    this(getOperatingSystemMXBean());
  }

  public FileDescriptorMetrics(OperatingSystemMXBean osMxBean) {

    this.osMxBean = osMxBean;
  }

  public Map<String, Metric> getMetrics() {
    final Map<String, Metric> gauges = new HashMap<>();
    gauges.put("open", (Gauge<Long>) () -> getMetricLong("getOpenFileDescriptorCount"));
    gauges.put("max", (Gauge<Long>) () -> getMetricLong("getMaxFileDescriptorCount"));
    return gauges;
  }

  private Long getMetricLong(String name) {
    try {
      return invoke(name);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      //e.printStackTrace();
      //TODO Log
      return -1L;
    }
  }

  private long invoke(String name)
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    final Method method = osMxBean.getClass().getDeclaredMethod(name);
    method.setAccessible(true);
    return (Long) method.invoke(osMxBean);
  }
}
