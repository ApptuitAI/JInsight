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

package ai.apptuit.metrics.jinsight.modules.ehcache;

import static org.junit.Assert.assertEquals;

import ai.apptuit.metrics.dropwizard.TagEncodedMetricName;
import ai.apptuit.metrics.jinsight.RegistryService;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


/**
 * @author Rajiv Shivane
 */
public class EHCacheInstrumentationTest {

  private String cacheName = "testCache." + UUID.randomUUID().toString();

  private MetricRegistry registry;
  private CacheManager cacheManager;
  private Ehcache cache;
  private Map<String, Object> presetElements;
  private List<String> presetElementKeys;

  @Before
  public void setUp() throws Exception {
    registry = RegistryService.getMetricRegistry();

    cacheManager = CacheManager.newInstance();
    cache = cacheManager.addCacheIfAbsent(cacheName);
    presetElements = IntStream.range(0, 1000).boxed()
        .collect(Collectors.toMap(i -> UUID.randomUUID().toString(), i -> i));
    presetElementKeys = new ArrayList<>(presetElements.keySet());

  }

  @Test
  public void testDefaultMetricsRegistration() throws Exception {
    Set<String> registeredGauges = registry.getGauges((name, metric) -> {
      return name.startsWith("ehcache.") && name.contains(cacheName);
    }).keySet();

    TagEncodedMetricName base = TagEncodedMetricName.decode("ehcache").withTags("cache", cacheName);
    String[] expectedGaugeNames = new String[]{"hits", "in_memory_hits", "off_heap_hits",
        "on_disk_hits", "misses", "in_memory_misses", "off_heap_misses", "on_disk_misses",
        "objects", "in_memory_objects", "off_heap_objects", "on_disk_objects", "mean_get_time",
        "mean_search_time", "eviction_count", "searches_per_second", "writer_queue_size"};
    Set<String> expectedGauges = new TreeSet<>();
    Arrays.asList(expectedGaugeNames)
        .forEach(gauge -> expectedGauges.add(base.submetric(gauge).toString()));

    assertEquals(expectedGauges, registeredGauges);
  }

  @Test
  public void testPut() throws Exception {
    int rnd = ThreadLocalRandom.current().nextInt(0, presetElements.size());
    String key = presetElementKeys.get(rnd);

    TagEncodedMetricName metricName = TagEncodedMetricName.decode(
        "ehcache.ops[op:put,cache:" + cacheName + "]");
    long expectedCount = getTimerCount(metricName) + 1;

    cache.put(new Element(key, presetElements.get(key)));

    assertEquals(expectedCount, getTimerCount(metricName));
  }

  @After
  public void tearDown() throws Exception {
    cacheManager.removeCache(cacheName);
    cacheManager.shutdown();
  }


  private long getTimerCount(TagEncodedMetricName name) {
    Timer timer = registry.getTimers().get(name.toString());
    return timer != null ? timer.getCount() : 0;
  }
}
