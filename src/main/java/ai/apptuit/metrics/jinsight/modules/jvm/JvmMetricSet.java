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

import static java.lang.management.ManagementFactory.getPlatformMBeanServer;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.jvm.BufferPoolMetricSet;
import com.codahale.metrics.jvm.CachedThreadStatesGaugeSet;
import com.codahale.metrics.jvm.ClassLoadingGaugeSet;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Rajiv Shivane
 */
public class JvmMetricSet implements MetricSet {

  private final Map<String, Metric> metrics = new HashMap<>();

  public JvmMetricSet() {
    registerSet("jvm.buffers", new BufferPoolMetricSet(getPlatformMBeanServer()));
    registerSet("jvm.classloading", new ClassLoadingGaugeSet());
    registerSet("jvm.fd", new FileDescriptorMetrics());
    registerSet("jvm.gc", new GarbageCollectorMetricSet());
    registerSet("jvm.memory", new MemoryUsageGaugeSet());
    registerSet("jvm.thread", new CachedThreadStatesGaugeSet(60, TimeUnit.SECONDS));

    metrics.put("jvm.uptime", new UptimeGauge());

    try {
      metrics.put("jvm.processCPU", new ProcessCpuTicksGauge());
    } catch (ClassNotFoundException | IOException e) {
      //e.printStackTrace();
      //TODO Log
    }
  }

  @Override
  public Map<String, Metric> getMetrics() {
    return Collections.unmodifiableMap(metrics);
  }

  private void registerSet(String prefix, MetricSet metricset) throws IllegalArgumentException {

    metricset.getMetrics().forEach((key, value) -> {
      metrics.put(getMetricName(prefix, key), value);
    });
  }

  private String getMetricName(String prefix, String key) {
    return MetricRegistry.name(prefix, key).toLowerCase().replace('-', '_');
  }
}
