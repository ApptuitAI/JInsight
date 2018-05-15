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
        "jvm.buffers.capacity[type:direct]",
        "jvm.buffers.capacity[type:mapped]",
        "jvm.buffers.count[type:direct]",
        "jvm.buffers.count[type:mapped]",
        "jvm.buffers.used[type:direct]",
        "jvm.buffers.used[type:mapped]",
        "jvm.classloading.loaded",
        "jvm.classloading.unloaded",
        "jvm.gc.count[generation:old,type:ps_marksweep]",
        "jvm.gc.time.millis[generation:old,type:ps_marksweep]",
        "jvm.gc.count[generation:young,type:ps_scavenge]",
        "jvm.gc.time.millis[generation:young,type:ps_scavenge]",
        "jvm.gc.total.count",
        "jvm.gc.total.time.millis",
        "jvm.memory.committed[type:heap]",
        "jvm.memory.committed[type:non_heap]",
        "jvm.memory.init[type:heap]",
        "jvm.memory.init[type:non_heap]",
        "jvm.memory.max[type:heap]",
        "jvm.memory.max[type:non_heap]",
        "jvm.memory.usage[type:heap]",
        "jvm.memory.usage[type:non_heap]",
        "jvm.memory.used[type:heap]",
        "jvm.memory.used[type:non_heap]",
        "jvm.memory.pools.committed[type:code_cache]",
        "jvm.memory.pools.committed[type:compressed_class_space]",
        "jvm.memory.pools.committed[type:metaspace]",
        "jvm.memory.pools.committed[type:ps_eden_space]",
        "jvm.memory.pools.committed[type:ps_old_gen]",
        "jvm.memory.pools.committed[type:ps_survivor_space]",
        "jvm.memory.pools.init[type:code_cache]",
        "jvm.memory.pools.init[type:compressed_class_space]",
        "jvm.memory.pools.init[type:metaspace]",
        "jvm.memory.pools.init[type:ps_eden_space]",
        "jvm.memory.pools.init[type:ps_old_gen]",
        "jvm.memory.pools.init[type:ps_survivor_space]",
        "jvm.memory.pools.max[type:code_cache]",
        "jvm.memory.pools.max[type:compressed_class_space]",
        "jvm.memory.pools.max[type:metaspace]",
        "jvm.memory.pools.max[type:ps_eden_space]",
        "jvm.memory.pools.max[type:ps_old_gen]",
        "jvm.memory.pools.max[type:ps_survivor_space]",
        "jvm.memory.pools.usage[type:code_cache]",
        "jvm.memory.pools.usage[type:compressed_class_space]",
        "jvm.memory.pools.usage[type:metaspace]",
        "jvm.memory.pools.usage[type:ps_eden_space]",
        "jvm.memory.pools.usage[type:ps_old_gen]",
        "jvm.memory.pools.usage[type:ps_survivor_space]",
        "jvm.memory.pools.used[type:code_cache]",
        "jvm.memory.pools.used[type:compressed_class_space]",
        "jvm.memory.pools.used[type:metaspace]",
        "jvm.memory.pools.used[type:ps_eden_space]",
        "jvm.memory.pools.used[type:ps_old_gen]",
        "jvm.memory.pools.used[type:ps_survivor_space]",
        "jvm.memory.pools.used_after_gc[type:ps_eden_space]",
        "jvm.memory.pools.used_after_gc[type:ps_old_gen]",
        "jvm.memory.pools.used_after_gc[type:ps_survivor_space]",
        "jvm.memory.total.committed",
        "jvm.memory.total.init",
        "jvm.memory.total.max",
        "jvm.memory.total.used",
        "jvm.process.cpu.nanos",
        "jvm.thread.blocked.count",
        "jvm.thread.count",
        "jvm.thread.daemon.count",
        "jvm.thread.deadlock.count",
        "jvm.thread.deadlocks",
        "jvm.thread.new.count",
        "jvm.thread.runnable.count",
        "jvm.thread.terminated.count",
        "jvm.thread.timed_waiting.count",
        "jvm.thread.waiting.count",
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
