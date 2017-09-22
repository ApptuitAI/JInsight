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

package ai.apptuit.metrics.jinsight.bci;

import static org.junit.Assert.assertEquals;

import ai.apptuit.metrics.dropwizard.TagEncodedMetricName;
import ai.apptuit.metrics.jinsight.RegistryService;
import ai.apptuit.metrics.util.MockMetricsRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.testng.PowerMockTestCase;


/**
 * @author Rajiv Shivane
 */
@PrepareForTest({RegistryService.class})
@PowerMockIgnore({"org.jboss.byteman.*", "javax.net.ssl.*"})
@RunWith(PowerMockRunner.class)
public class EHCacheInstrumentationTest extends PowerMockTestCase {

  private String cacheName = "testCache." + UUID.randomUUID().toString();
  private CacheManager cacheManager;
  private Ehcache cache;
  private Map<String, Object> presetElements;
  private List<String> presetElementKeys;
  private MockMetricsRegistry metricsRegistry;

  @Before
  public void setUp() throws Exception {
    metricsRegistry = MockMetricsRegistry.getInstance();

    cacheManager = CacheManager.newInstance();
    cache = cacheManager.addCacheIfAbsent(cacheName);
    presetElements = IntStream.range(0, 1000).boxed()
        .collect(Collectors.toMap(i -> UUID.randomUUID().toString(), i -> i));
    presetElementKeys = new ArrayList<>(presetElements.keySet());

  }

  @Test
  public void testPut() throws Exception {
    int rnd = ThreadLocalRandom.current().nextInt(0, presetElements.size());
    String key = presetElementKeys.get(rnd);

    TagEncodedMetricName metricName = TagEncodedMetricName.decode(
        "ehcache.ops[op:put,cache:" + cacheName + "]");
    assertEquals(0, metricsRegistry.getStartCount(metricName));
    assertEquals(0, metricsRegistry.getStopCount(metricName));

    cache.put(new Element(key, presetElements.get(key)));

    assertEquals(1, metricsRegistry.getStartCount(metricName));
    assertEquals(1, metricsRegistry.getStopCount(metricName));
  }

  @After
  public void tearDown() throws Exception {
    cacheManager.removeCache(cacheName);
    cacheManager.shutdown();
  }
}
