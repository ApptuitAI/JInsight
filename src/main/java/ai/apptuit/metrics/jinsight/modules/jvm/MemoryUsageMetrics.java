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

import ai.apptuit.metrics.dropwizard.TagEncodedMetricName;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.RatioGauge;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author Rajiv Shivane
 */
class MemoryUsageMetrics implements MetricSet {

  private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");

  private final MemoryMXBean mxBean;
  private final List<MemoryPoolMXBean> memoryPools;

  public MemoryUsageMetrics() {
    this(ManagementFactory.getMemoryMXBean(), ManagementFactory.getMemoryPoolMXBeans());
  }

  public MemoryUsageMetrics(MemoryMXBean mxBean, Collection<MemoryPoolMXBean> memoryPools) {
    this.mxBean = mxBean;
    this.memoryPools = new ArrayList<>(memoryPools);
  }

  @Override
  public Map<String, Metric> getMetrics() {
    final Map<String, Metric> gauges = new HashMap<>();

    gauges.put("total.init.bytes", (Gauge<Long>) () ->
        mxBean.getHeapMemoryUsage().getInit() + mxBean.getNonHeapMemoryUsage().getInit()
    );

    gauges.put("total.used.bytes", (Gauge<Long>) () ->
        mxBean.getHeapMemoryUsage().getUsed() + mxBean.getNonHeapMemoryUsage().getUsed()
    );

    gauges.put("total.max.bytes", (Gauge<Long>) () ->
        mxBean.getHeapMemoryUsage().getMax() + mxBean.getNonHeapMemoryUsage().getMax()
    );

    gauges.put("total.committed.bytes", (Gauge<Long>) () ->
        mxBean.getHeapMemoryUsage().getCommitted() + mxBean.getNonHeapMemoryUsage().getCommitted()
    );

    gauges.put(getMetricName("init.bytes", "type", "heap"),
        (Gauge<Long>) () -> mxBean.getHeapMemoryUsage().getInit());

    gauges.put(getMetricName("used.bytes", "type", "heap"),
        (Gauge<Long>) () -> mxBean.getHeapMemoryUsage().getUsed());

    gauges.put(getMetricName("max.bytes", "type", "heap"),
        (Gauge<Long>) () -> mxBean.getHeapMemoryUsage().getMax());

    gauges.put(getMetricName("committed.bytes", "type", "heap"),
        (Gauge<Long>) () -> mxBean.getHeapMemoryUsage().getCommitted());

    gauges.put(getMetricName("usage", "type", "heap"), new RatioGauge() {
      @Override
      protected Ratio getRatio() {
        final MemoryUsage usage = mxBean.getHeapMemoryUsage();
        return Ratio.of(usage.getUsed(), usage.getMax());
      }
    });

    gauges.put(getMetricName("init.bytes", "type", "non-heap"),
        (Gauge<Long>) () -> mxBean.getNonHeapMemoryUsage().getInit());

    gauges.put(getMetricName("used.bytes", "type", "non-heap"),
        (Gauge<Long>) () -> mxBean.getNonHeapMemoryUsage().getUsed());

    gauges.put(getMetricName("max.bytes", "type", "non-heap"),
        (Gauge<Long>) () -> mxBean.getNonHeapMemoryUsage().getMax());

    gauges.put(getMetricName("committed.bytes", "type", "non-heap"),
        (Gauge<Long>) () -> mxBean.getNonHeapMemoryUsage().getCommitted());

    gauges.put(getMetricName("usage", "type", "non-heap"), new RatioGauge() {
      @Override
      protected Ratio getRatio() {
        final MemoryUsage usage = mxBean.getNonHeapMemoryUsage();
        return Ratio.of(usage.getUsed(), usage.getMax());
      }
    });

    for (final MemoryPoolMXBean pool : memoryPools) {
      final String poolName = WHITESPACE.matcher(pool.getName()).replaceAll("_").toLowerCase();

      gauges.put(getMetricName("pools.usage", "type", poolName),
          new RatioGauge() {
            @Override
            protected Ratio getRatio() {
              MemoryUsage usage = pool.getUsage();
              return Ratio.of(usage.getUsed(),
                  usage.getMax() == -1 ? usage.getCommitted() : usage.getMax());
            }
          });

      gauges.put(getMetricName("pools.max.bytes", "type", poolName),
          (Gauge<Long>) () -> pool.getUsage().getMax());

      gauges.put(getMetricName("pools.used.bytes", "type", poolName),
          (Gauge<Long>) () -> pool.getUsage().getUsed());

      gauges.put(getMetricName("pools.committed.bytes", "type", poolName),
          (Gauge<Long>) () -> pool.getUsage().getCommitted());

      // Only register GC usage metrics if the memory pool supports usage statistics.
      if (pool.getCollectionUsage() != null) {
        gauges.put(getMetricName("pools.used-after-gc.bytes", "type", poolName),
            (Gauge<Long>) () -> pool.getCollectionUsage().getUsed());
      }

      gauges.put(getMetricName("pools.init.bytes", "type", poolName),
          (Gauge<Long>) () -> pool.getUsage().getInit());
    }

    return Collections.unmodifiableMap(gauges);
  }

  private String getMetricName(String base, String... tags) {
    return TagEncodedMetricName.decode(base).withTags(tags).toString();
  }
}
