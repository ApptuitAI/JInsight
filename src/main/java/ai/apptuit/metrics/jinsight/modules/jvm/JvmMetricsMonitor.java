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

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.jvm.BufferPoolMetricSet;
import com.codahale.metrics.jvm.CachedThreadStatesGaugeSet;
import com.codahale.metrics.jvm.ClassLoadingGaugeSet;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @author Rajiv Shivane
 */
public class JvmMetricsMonitor {

  private final MetricRegistry metricRegistry;

  public JvmMetricsMonitor(MetricRegistry registry) {
    this.metricRegistry = registry;
    registerAll();
  }


  private void registerAll() {

    registerAll("jvm.buffers", new BufferPoolMetricSet(getPlatformMBeanServer()));
    registerAll("jvm.classloading", new ClassLoadingGaugeSet());
    registerAll("jvm.fd", new FileDescriptorMetrics());
    registerAll("jvm.gc", new GarbageCollectorMetricSet());
    registerAll("jvm.memory", new MemoryUsageGaugeSet());
    registerAll("jvm.thread", new CachedThreadStatesGaugeSet(60, TimeUnit.SECONDS));

    metricRegistry.register("jvm.uptime", new UptimeGauge());

    try {
      metricRegistry.register("jvm.processCPU", new ProcessCpuTicksGauge());
    } catch (ClassNotFoundException | IOException e) {
      //e.printStackTrace();
      //TODO Log
    }
  }

  private void registerAll(String prefix, MetricSet metrics)
      throws IllegalArgumentException {
    metrics.getMetrics().forEach((key, value) -> {
      metricRegistry.register(getMetricName(prefix, key), value);
    });
  }

  private String getMetricName(String prefix, String key) {
    return MetricRegistry.name(prefix, key).toLowerCase().replace('-', '_');
  }


}
