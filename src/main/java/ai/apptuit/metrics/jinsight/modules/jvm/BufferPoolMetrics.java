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

import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Rajiv Shivane
 */
class BufferPoolMetrics implements MetricSet {

  private static final String POOL_TAG_NAME = "pool";

  private final List<BufferPoolMXBean> pools;

  public BufferPoolMetrics() {
    pools = ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class);
  }

  @Override
  public Map<String, Metric> getMetrics() {
    final Map<String, Metric> gauges = new HashMap<>();

    for (BufferPoolMXBean pool : pools) {
      final String poolName = pool.getName();
      gauges.put(getMetricName("capacity.bytes", POOL_TAG_NAME, poolName),
              (Gauge<Long>) pool::getTotalCapacity);
      gauges.put(getMetricName("count", POOL_TAG_NAME, poolName),
              (Gauge<Long>) pool::getCount);
      gauges.put(getMetricName("used.bytes", POOL_TAG_NAME, poolName),
              (Gauge<Long>) pool::getMemoryUsed);
    }

    return Collections.unmodifiableMap(gauges);
  }

  private String getMetricName(String base, String... tags) {
    return TagEncodedMetricName.decode(base).withTags(tags).toString();
  }
}