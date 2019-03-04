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

import static java.lang.management.ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME;
import static java.lang.management.ManagementFactory.getPlatformMBeanServer;
import static java.lang.management.ManagementFactory.getRuntimeMXBean;
import static java.lang.management.ManagementFactory.newPlatformMXBeanProxy;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.jvm.ClassLoadingGaugeSet;

import java.io.IOException;
import java.lang.management.RuntimeMXBean;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.MBeanServerConnection;

/**
 * @author Rajiv Shivane
 */
public class JvmMetricSet implements MetricSet {

  private static final Logger LOGGER = Logger.getLogger(JvmMetricSet.class.getName());

  private final Map<String, Metric> metrics = new HashMap<>();

  public JvmMetricSet() {
    registerSet("jvm.buffer_pool", new BufferPoolMetrics());
    registerSet("jvm.classes", new ClassLoadingGaugeSet());
    registerSet("jvm.fd", new FileDescriptorMetrics());
    registerSet("jvm.gc", new GarbageCollectorMetrics());
    registerSet("jvm.memory", new MemoryUsageMetrics());
    registerSet("jvm.threads", new ThreadStateMetrics());

    metrics.put("jvm.uptime.seconds", new UptimeGauge());

    try {
      metrics.put("jvm.process.cpu.seconds", new ProcessCpuTicksGauge());
    } catch (ClassNotFoundException | IOException e) {
      LOGGER.log(Level.SEVERE, "Error fetching process CPU usage metrics.", e);
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

  private static class UptimeGauge implements Gauge<Double> {

    private final RuntimeMXBean rtMxBean = getRuntimeMXBean();

    @Override
    public Double getValue() {
      // converting millis into seconds
      return rtMxBean.getUptime() / 1000.0;
    }
  }

  private static class ProcessCpuTicksGauge implements Gauge<Double> {

    private final com.sun.management.OperatingSystemMXBean osMxBean;

    public ProcessCpuTicksGauge() throws ClassNotFoundException, IOException {

      MBeanServerConnection mbsc = getPlatformMBeanServer();
      Class.forName("com.sun.management.OperatingSystemMXBean");

      osMxBean = newPlatformMXBeanProxy(mbsc, OPERATING_SYSTEM_MXBEAN_NAME,
              com.sun.management.OperatingSystemMXBean.class);

    }

    @Override
    public Double getValue() {
      //converting nanoseconds into seconds
      return osMxBean.getProcessCpuTime() / 1000000000.0;
    }
  }
}