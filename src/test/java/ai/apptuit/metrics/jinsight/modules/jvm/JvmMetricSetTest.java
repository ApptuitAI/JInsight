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
        "jvm.buffers.capacity.bytes[type:direct]",
        "jvm.buffers.capacity.bytes[type:mapped]",
        "jvm.buffers.count[type:direct]",
        "jvm.buffers.count[type:mapped]",
        "jvm.buffers.used.bytes[type:direct]",
        "jvm.buffers.used.bytes[type:mapped]",
        "jvm.classloading.loaded",
        "jvm.classloading.unloaded",
        "jvm.gc.count[generation:old,type:ps_marksweep]",
        "jvm.gc.time.millis[generation:old,type:ps_marksweep]",
        "jvm.gc.count[generation:young,type:ps_scavenge]",
        "jvm.gc.time.millis[generation:young,type:ps_scavenge]",
        "jvm.gc.total.count",
        "jvm.gc.total.time.millis",
        "jvm.memory.committed.bytes[type:heap]",
        "jvm.memory.committed.bytes[type:non_heap]",
        "jvm.memory.init.bytes[type:heap]",
        "jvm.memory.init.bytes[type:non_heap]",
        "jvm.memory.max.bytes[type:heap]",
        "jvm.memory.max.bytes[type:non_heap]",
        "jvm.memory.usage[type:heap]",
        "jvm.memory.usage[type:non_heap]",
        "jvm.memory.used.bytes[type:heap]",
        "jvm.memory.used.bytes[type:non_heap]",
        "jvm.memory.pools.committed.bytes[type:code_cache]",
        "jvm.memory.pools.committed.bytes[type:compressed_class_space]",
        "jvm.memory.pools.committed.bytes[type:metaspace]",
        "jvm.memory.pools.committed.bytes[type:ps_eden_space]",
        "jvm.memory.pools.committed.bytes[type:ps_old_gen]",
        "jvm.memory.pools.committed.bytes[type:ps_survivor_space]",
        "jvm.memory.pools.init.bytes[type:code_cache]",
        "jvm.memory.pools.init.bytes[type:compressed_class_space]",
        "jvm.memory.pools.init.bytes[type:metaspace]",
        "jvm.memory.pools.init.bytes[type:ps_eden_space]",
        "jvm.memory.pools.init.bytes[type:ps_old_gen]",
        "jvm.memory.pools.init.bytes[type:ps_survivor_space]",
        "jvm.memory.pools.max.bytes[type:code_cache]",
        "jvm.memory.pools.max.bytes[type:compressed_class_space]",
        "jvm.memory.pools.max.bytes[type:metaspace]",
        "jvm.memory.pools.max.bytes[type:ps_eden_space]",
        "jvm.memory.pools.max.bytes[type:ps_old_gen]",
        "jvm.memory.pools.max.bytes[type:ps_survivor_space]",
        "jvm.memory.pools.usage[type:code_cache]",
        "jvm.memory.pools.usage[type:compressed_class_space]",
        "jvm.memory.pools.usage[type:metaspace]",
        "jvm.memory.pools.usage[type:ps_eden_space]",
        "jvm.memory.pools.usage[type:ps_old_gen]",
        "jvm.memory.pools.usage[type:ps_survivor_space]",
        "jvm.memory.pools.used.bytes[type:code_cache]",
        "jvm.memory.pools.used.bytes[type:compressed_class_space]",
        "jvm.memory.pools.used.bytes[type:metaspace]",
        "jvm.memory.pools.used.bytes[type:ps_eden_space]",
        "jvm.memory.pools.used.bytes[type:ps_old_gen]",
        "jvm.memory.pools.used.bytes[type:ps_survivor_space]",
        "jvm.memory.pools.used_after_gc.bytes[type:ps_eden_space]",
        "jvm.memory.pools.used_after_gc.bytes[type:ps_old_gen]",
        "jvm.memory.pools.used_after_gc.bytes[type:ps_survivor_space]",
        "jvm.memory.total.committed.bytes",
        "jvm.memory.total.init.bytes",
        "jvm.memory.total.max.bytes",
        "jvm.memory.total.used.bytes",
        "jvm.process.cpu.nanos",
        "jvm.thread.count[state:blocked]",
        "jvm.thread.count[state:new]",
        "jvm.thread.count[state:runnable]",
        "jvm.thread.count[state:terminated]",
        "jvm.thread.count[state:timed_waiting]",
        "jvm.thread.count[state:waiting]",
        "jvm.thread.total.count",
        "jvm.thread.daemon.count",
        "jvm.thread.deadlock.count",
        "jvm.thread.deadlocks",
        "jvm.uptime.millis"
    );
    TreeSet<String> expectedMetrics = new TreeSet<>(expected);

    String osName = System.getProperty("os.name").toLowerCase();
    if (!osName.contains("windows")) {
      expectedMetrics.add("jvm.fd.max");
      expectedMetrics.add("jvm.fd.open");
    }

    assertEquals(expectedMetrics, registry.getNames());
  }
}
