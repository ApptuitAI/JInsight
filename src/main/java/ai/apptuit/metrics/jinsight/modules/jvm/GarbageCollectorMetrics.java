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
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
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
class GarbageCollectorMetrics implements MetricSet {

  private static final String YOUNG_GEN = "young";
  private static final String OLD_GEN = "old";
  private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");

  private final List<GarbageCollector> garbageCollectors;

  public GarbageCollectorMetrics() {
    this(ManagementFactory.getGarbageCollectorMXBeans());
  }

  public GarbageCollectorMetrics(Collection<GarbageCollectorMXBean> collectorMxBeans) {
    this.garbageCollectors = new ArrayList<>(collectorMxBeans.size());
    collectorMxBeans.stream().map(GarbageCollector::new).forEach(this.garbageCollectors::add);
  }

  @Override
  public Map<String, Metric> getMetrics() {
    final Map<String, Metric> gauges = new HashMap<>();
    garbageCollectors.forEach(gc -> {
      gauges.put(getMetricName(gc, "count"), (Gauge<Long>) gc::getCollectionCount);
      gauges.put(getMetricName(gc, "time.millis"), (Gauge<Long>) gc::getCollectionTime);
    });
    gauges.put("total.count", (Gauge<Long>) () -> {
      long count = 0;
      for (GarbageCollector gc : garbageCollectors) {
        count += gc.getCollectionCount();
      }
      return count;
    });

    gauges.put("total.time.millis", (Gauge<Long>) () -> {
      long time = 0;
      for (GarbageCollector gc : garbageCollectors) {
        time += gc.getCollectionTime();
      }
      return time;
    });
    return Collections.unmodifiableMap(gauges);
  }

  private String getMetricName(GarbageCollector gc, String submetric) {
    return TagEncodedMetricName.decode(submetric)
        .withTags("type", gc.getName())
        .withTags("generation", gc.getGcGeneration())
        .toString();
  }

  private static class GarbageCollector {

    private GarbageCollectorMXBean delegate;

    GarbageCollector(GarbageCollectorMXBean delegate) {
      this.delegate = delegate;

    }

    public String getGcGeneration() {
      String name = delegate.getName();
      switch (name) {
        case "Copy":
        case "PS Scavenge":
        case "ParNew":
        case "G1 Young Generation":
          return YOUNG_GEN;
        case "MarkSweepCompact":
        case "PS MarkSweep":
        case "ConcurrentMarkSweep":
        case "G1 Old Generation":
          return OLD_GEN;
        default:
          return name;
      }
    }

    public Long getCollectionCount() {
      return delegate.getCollectionCount();
    }

    public Long getCollectionTime() {
      return delegate.getCollectionTime();
    }

    public String getName() {
      return WHITESPACE.matcher(delegate.getName()).replaceAll("_").toLowerCase();
    }
  }
}
