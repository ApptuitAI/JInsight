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

package ai.apptuit.metrics.jinsight.testing;

import static org.junit.Assert.assertEquals;

import ai.apptuit.metrics.client.TagEncodedMetricName;
import com.codahale.metrics.Counting;
import com.codahale.metrics.MetricRegistry;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * @author Rajiv Shivane
 */
public class CountTracker {

  private MetricRegistry registry;
  private TagEncodedMetricName rootMetric;
  private String[] tags;

  private Map<String, Counting> counters;

  public CountTracker(MetricRegistry registry, TagEncodedMetricName rootMetric, String... tags) {
    this.registry = registry;
    this.rootMetric = rootMetric;
    this.tags = tags;

    counters = new HashMap<>();
  }

  public void registerTimer(Object... tagValues) {
    register(tagValues, metricName -> registry.timer(metricName.toString()));
  }

  public void registerMeter(Object... tagValues) {
    register(tagValues, metricName -> registry.meter(metricName.toString()));
  }

  private void register(Object[] tagValues,
      Function<TagEncodedMetricName, Counting> countingSupplier) {
    String key = getKey(tagValues);
    Map<String, String> tagMap = new HashMap<>(tagValues.length);
    for (int i = 0; i < tags.length; i++) {
      tagMap.put(tags[i], String.valueOf(tagValues[i]));
    }
    counters.put(key, countingSupplier.apply(rootMetric.withTags(tagMap)));
  }

  private String getKey(Object[] tagValues) {
    if (tagValues.length != tags.length) {
      throw new IllegalArgumentException(
          "Tag values should correspond to the tags: " + Arrays.asList(tags));
    }

    StringBuilder keyBuilder = new StringBuilder();
    for (int i = 0; i < tags.length; i++) {
      Object value = tagValues[i];
      if (!(value instanceof Number) && !(value instanceof String)) {
        throw new IllegalArgumentException("values must be String or number");
      }
      if (i != 0) {
        keyBuilder.append(':');
      }
      keyBuilder.append(String.valueOf(value));
    }
    String s = keyBuilder.toString();
    return s.length() == 0 ? rootMetric.toString() : s;
  }

  public Snapshot snapshot() {
    return new Snapshot(getCurrentCounts());
  }

  private Map<String, Long> getCurrentCounts() {
    Map<String, Long> currentCounts = new TreeMap<>();
    counters.forEach((k, counting) -> {
      currentCounts.put(k, counting.getCount());
    });
    return currentCounts;
  }

  public void validate(Snapshot snapshot) {
    snapshot.validate();
  }

  public class Snapshot {

    private final Map<String, Long> snapshotValues;
    private final Map<String, Long> incrementValues;

    private Snapshot(Map<String, Long> currentCounts) {
      snapshotValues = currentCounts;
      incrementValues = new HashMap<>();
    }

    public void increment(Object... tagValues) {
      increment(1, tagValues);
    }

    public void increment(Integer increment, Object... tagValues) {
      String key = getKey(tagValues);
      if (!counters.containsKey(key)) {
        throw new IllegalArgumentException("No counter registered for key ["
            + key + "]. Register it by calling register method first.");
      }
      incrementValues.compute(key, (s, aLong) -> aLong != null ? aLong + increment : increment);
    }

    private Map<String, Long> getExpectedCounts() {
      Map<String, Long> expectedCounts = new TreeMap<>(snapshotValues);
      incrementValues.forEach((key, aLong) -> {
        expectedCounts.compute(key, (s, aLong1) -> aLong + aLong1);
      });
      return expectedCounts;
    }

    public void validate() {
      Map<String, Long> currentCounts = getCurrentCounts();
      Map<String, Long> expectedCounts = getExpectedCounts();
      assertEquals(expectedCounts, currentCounts);
    }
  }
}
