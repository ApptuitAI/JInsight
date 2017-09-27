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

import ai.apptuit.metrics.jinsight.modules.jvm.JvmMetricsMonitor;
import com.codahale.metrics.MetricRegistry;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;
import org.junit.Before;
import org.junit.Test;


/**
 * @author Rajiv Shivane
 */
public class JVMMetricsMonitorTest {

  private MetricRegistry registry;

  @Before
  public void setUp() throws Exception {
    registry = new MetricRegistry();
    JvmMetricsMonitor monitor = new JvmMetricsMonitor(registry);
  }

  @Test
  public void testJVMMetrics() throws Exception {
    List<String> expected = Arrays.asList("jvm.buffers.direct.capacity",
        "jvm.buffers.direct.count",
        "jvm.buffers.direct.used",
        "jvm.buffers.mapped.capacity",
        "jvm.buffers.mapped.count",
        "jvm.buffers.mapped.used",
        "jvm.classloading.loaded",
        "jvm.classloading.unloaded",
        "jvm.fd.max",
        "jvm.fd.open",
        "jvm.gc.ps_marksweep.count",
        "jvm.gc.ps_marksweep.time",
        "jvm.gc.ps_scavenge.count",
        "jvm.gc.ps_scavenge.time",
        "jvm.memory.heap.committed",
        "jvm.memory.heap.init",
        "jvm.memory.heap.max",
        "jvm.memory.heap.usage",
        "jvm.memory.heap.used",
        "jvm.memory.non_heap.committed",
        "jvm.memory.non_heap.init",
        "jvm.memory.non_heap.max",
        "jvm.memory.non_heap.usage",
        "jvm.memory.non_heap.used",
        "jvm.memory.pools.code_cache.committed",
        "jvm.memory.pools.code_cache.init",
        "jvm.memory.pools.code_cache.max",
        "jvm.memory.pools.code_cache.usage",
        "jvm.memory.pools.code_cache.used",
        "jvm.memory.pools.compressed_class_space.committed",
        "jvm.memory.pools.compressed_class_space.init",
        "jvm.memory.pools.compressed_class_space.max",
        "jvm.memory.pools.compressed_class_space.usage",
        "jvm.memory.pools.compressed_class_space.used",
        "jvm.memory.pools.metaspace.committed",
        "jvm.memory.pools.metaspace.init",
        "jvm.memory.pools.metaspace.max",
        "jvm.memory.pools.metaspace.usage",
        "jvm.memory.pools.metaspace.used",
        "jvm.memory.pools.ps_eden_space.committed",
        "jvm.memory.pools.ps_eden_space.init",
        "jvm.memory.pools.ps_eden_space.max",
        "jvm.memory.pools.ps_eden_space.usage",
        "jvm.memory.pools.ps_eden_space.used",
        "jvm.memory.pools.ps_eden_space.used_after_gc",
        "jvm.memory.pools.ps_old_gen.committed",
        "jvm.memory.pools.ps_old_gen.init",
        "jvm.memory.pools.ps_old_gen.max",
        "jvm.memory.pools.ps_old_gen.usage",
        "jvm.memory.pools.ps_old_gen.used",
        "jvm.memory.pools.ps_old_gen.used_after_gc",
        "jvm.memory.pools.ps_survivor_space.committed",
        "jvm.memory.pools.ps_survivor_space.init",
        "jvm.memory.pools.ps_survivor_space.max",
        "jvm.memory.pools.ps_survivor_space.usage",
        "jvm.memory.pools.ps_survivor_space.used",
        "jvm.memory.pools.ps_survivor_space.used_after_gc",
        "jvm.memory.total.committed",
        "jvm.memory.total.init",
        "jvm.memory.total.max",
        "jvm.memory.total.used",
        "jvm.processCPU",
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
        "jvm.uptime"
    );
    assertEquals(new TreeSet<>(expected), registry.getNames());
  }
}
