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
package ai.apptuit.metrics.jinsight;

import static org.awaitility.Awaitility.with;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;

/**
 * @author Rajiv Shivane
 */
public class MetricRegistryCollectionIT {

  private static final boolean DEBUG = false;

  @Test
  public void testGC() throws Exception {
    AtomicBoolean finalized = new AtomicBoolean(false);

    String counterName = "test.counter";
    long value = 7;

    MetricRegistryCollection metricRegistryCollection = MetricRegistryCollection.getInstance();
    Class<? extends MetricRegistryCollection> jirClass = metricRegistryCollection.getClass();
    Method getAggregatedMetricRegistry = jirClass.getDeclaredMethod("getAggregatedMetricRegistry");
    getAggregatedMetricRegistry.setAccessible(true);
    Object registryCollection = getAggregatedMetricRegistry.invoke(metricRegistryCollection);
    Method getCounters = registryCollection.getClass().getMethod("getCounters");

    registerGCableRegistry(finalized, counterName, value);
    Map<String, Object> counters = (Map<String, Object>) getCounters.invoke(registryCollection);
    Object counter = counters.get(counterName);
    assertNotNull(counter);
    Method getCount = counter.getClass().getMethod("getCount");
    assertEquals(value, getCount.invoke(counter));

    with()
        .conditionEvaluationListener(condition -> {
          if (condition.isSatisfied()) {
            debug("Registry GC'ed. DONE!");
          } else {
            debug("Requesting GC");
            System.gc();
          }
        })
        .await().atMost(Duration.ofMinutes(1)).untilTrue(finalized);
  }


  private void registerGCableRegistry(AtomicBoolean finalized, String name, long value) {
    MetricRegistry registry = new MetricRegistry() {
      @Override
      protected void finalize() throws Throwable {
        super.finalize();
        debug("Registry Finalized!");
        finalized.set(true);
      }
    };
    MetricRegistryCollection instance = MetricRegistryCollection.getInstance();
    instance.register(registry);

    Counter counter = registry.counter(name);
    counter.inc(value);
    Thread referenceHolder = new Thread() {
      @Override
      public void run() {
        super.run();
        try {
          debug("Holding reference to ["
              + registry.getCounters((name1, metric) -> name1.equals(name)).size()
              + "] counters.\n");
          Thread.sleep(1000);
          debug("Releasing reference");
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    };
    referenceHolder.start();
  }

  private void debug(String message) {
    if (DEBUG) {
      System.out.println(message);
    }
  }
}
