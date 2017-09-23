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

package ai.apptuit.metrics.jinsight.core;

import static java.lang.management.ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME;
import static java.lang.management.ManagementFactory.getOperatingSystemMXBean;
import static java.lang.management.ManagementFactory.getPlatformMBeanServer;
import static java.lang.management.ManagementFactory.getRuntimeMXBean;
import static java.lang.management.ManagementFactory.newPlatformMXBeanProxy;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.jvm.BufferPoolMetricSet;
import com.codahale.metrics.jvm.CachedThreadStatesGaugeSet;
import com.codahale.metrics.jvm.ClassLoadingGaugeSet;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import java.io.IOException;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.management.MBeanServerConnection;

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


  private static class FileDescriptorMetrics implements MetricSet {

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


  private static class UptimeGauge implements Gauge<Long> {

    private final RuntimeMXBean rtMxBean = getRuntimeMXBean();

    @Override
    public Long getValue() {
      return rtMxBean.getUptime();
    }
  }

  private class ProcessCpuTicksGauge implements Gauge<Long> {

    private final com.sun.management.OperatingSystemMXBean osMxBean;

    public ProcessCpuTicksGauge() throws ClassNotFoundException, IOException {

      MBeanServerConnection mbsc = getPlatformMBeanServer();
      Class.forName("com.sun.management.OperatingSystemMXBean");

      osMxBean = newPlatformMXBeanProxy(mbsc, OPERATING_SYSTEM_MXBEAN_NAME,
          com.sun.management.OperatingSystemMXBean.class);

    }

    @Override
    public Long getValue() {
      return osMxBean.getProcessCpuTime();
    }
  }
}
