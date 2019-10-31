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

import static ai.apptuit.metrics.jinsight.modules.jvm.JvmMetricSet.JVM_INFO_METRIC_NAME;
import static org.junit.Assert.assertEquals;

import com.codahale.metrics.MetricRegistry;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;
import org.junit.Before;
import org.junit.Test;


/**
 * @author Rajiv Shivane
 */
public class JvmMetricSetTest {

  private MetricRegistry registry;

  @Before
  public void setUp() throws Exception {
    registry = new MetricRegistry();
    registry.registerAll(new JvmMetricSet());
  }

  @Test
  public void testJVMMetrics() throws Exception {
    List<String> expected = Arrays.asList(
            "jvm.buffer_pool.capacity.bytes[pool:direct]",
            "jvm.buffer_pool.capacity.bytes[pool:mapped]",
            "jvm.buffer_pool.count[pool:direct]",
            "jvm.buffer_pool.count[pool:mapped]",
            "jvm.buffer_pool.used.bytes[pool:direct]",
            "jvm.buffer_pool.used.bytes[pool:mapped]",
            "jvm.classes.loaded",
            "jvm.classes.unloaded",
            "jvm.gc.count[generation:old,type:ps_marksweep]",
            "jvm.gc.time.seconds[generation:old,type:ps_marksweep]",
            "jvm.gc.count[generation:young,type:ps_scavenge]",
            "jvm.gc.time.seconds[generation:young,type:ps_scavenge]",
            "jvm.gc.total.count",
            "jvm.gc.total.time.seconds",
            "jvm.memory.committed.bytes[area:heap]",
            "jvm.memory.committed.bytes[area:non_heap]",
            "jvm.memory.init.bytes[area:heap]",
            "jvm.memory.init.bytes[area:non_heap]",
            "jvm.memory.max.bytes[area:heap]",
            "jvm.memory.max.bytes[area:non_heap]",
            "jvm.memory.usage[area:heap]",
            "jvm.memory.usage[area:non_heap]",
            "jvm.memory.used.bytes[area:heap]",
            "jvm.memory.used.bytes[area:non_heap]",
            "jvm.memory.pool.committed.bytes[pool:code_cache]",
            "jvm.memory.pool.committed.bytes[pool:compressed_class_space]",
            "jvm.memory.pool.committed.bytes[pool:metaspace]",
            "jvm.memory.pool.committed.bytes[name:ps_eden_space,pool:eden]",
            "jvm.memory.pool.committed.bytes[name:ps_old_gen,pool:old]",
            "jvm.memory.pool.committed.bytes[name:ps_survivor_space,pool:survivor]",
            "jvm.memory.pool.init.bytes[pool:code_cache]",
            "jvm.memory.pool.init.bytes[pool:compressed_class_space]",
            "jvm.memory.pool.init.bytes[pool:metaspace]",
            "jvm.memory.pool.init.bytes[name:ps_eden_space,pool:eden]",
            "jvm.memory.pool.init.bytes[name:ps_old_gen,pool:old]",
            "jvm.memory.pool.init.bytes[name:ps_survivor_space,pool:survivor]",
            "jvm.memory.pool.max.bytes[pool:code_cache]",
            "jvm.memory.pool.max.bytes[pool:compressed_class_space]",
            "jvm.memory.pool.max.bytes[pool:metaspace]",
            "jvm.memory.pool.max.bytes[name:ps_eden_space,pool:eden]",
            "jvm.memory.pool.max.bytes[name:ps_old_gen,pool:old]",
            "jvm.memory.pool.max.bytes[name:ps_survivor_space,pool:survivor]",
            "jvm.memory.pool.usage[pool:code_cache]",
            "jvm.memory.pool.usage[pool:compressed_class_space]",
            "jvm.memory.pool.usage[pool:metaspace]",
            "jvm.memory.pool.usage[name:ps_eden_space,pool:eden]",
            "jvm.memory.pool.usage[name:ps_old_gen,pool:old]",
            "jvm.memory.pool.usage[name:ps_survivor_space,pool:survivor]",
            "jvm.memory.pool.used.bytes[pool:code_cache]",
            "jvm.memory.pool.used.bytes[pool:compressed_class_space]",
            "jvm.memory.pool.used.bytes[pool:metaspace]",
            "jvm.memory.pool.used.bytes[name:ps_eden_space,pool:eden]",
            "jvm.memory.pool.used.bytes[name:ps_old_gen,pool:old]",
            "jvm.memory.pool.used.bytes[name:ps_survivor_space,pool:survivor]",
            "jvm.memory.pool.used_after_gc.bytes[name:ps_eden_space,pool:eden]",
            "jvm.memory.pool.used_after_gc.bytes[name:ps_old_gen,pool:old]",
            "jvm.memory.pool.used_after_gc.bytes[name:ps_survivor_space,pool:survivor]",
            "jvm.memory.total.committed.bytes",
            "jvm.memory.total.init.bytes",
            "jvm.memory.total.max.bytes",
            "jvm.memory.total.used.bytes",
            "jvm.process.cpu.seconds",
            "jvm.threads.count[state:blocked]",
            "jvm.threads.count[state:new]",
            "jvm.threads.count[state:runnable]",
            "jvm.threads.count[state:terminated]",
            "jvm.threads.count[state:timed_waiting]",
            "jvm.threads.count[state:waiting]",
            "jvm.threads.total.count",
            "jvm.threads.daemon.count",
            "jvm.threads.deadlock.count",
            "jvm.threads.deadlocks",
            "jvm.uptime.seconds"
    );
    TreeSet<String> expectedMetrics = new TreeSet<>(expected);

    String osName = System.getProperty("os.name").toLowerCase();
    if (!osName.contains("windows")) {
      expectedMetrics.add("jvm.fd.max");
      expectedMetrics.add("jvm.fd.open");
    }

    expectedMetrics.add(JVM_INFO_METRIC_NAME);

    assertEquals(expectedMetrics, registry.getNames());
  }
}