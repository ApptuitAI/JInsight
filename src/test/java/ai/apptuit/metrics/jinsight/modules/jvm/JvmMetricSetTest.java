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


  private static final String [] JDK8_MEM_METRICS = {
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
    "jvm.memory.pool.used_after_gc.bytes[name:ps_survivor_space,pool:survivor]"
  };

  private static final String [] JDK11_MEM_METRICS = {
    "jvm.memory.pool.committed.bytes[name:g1_eden_space,pool:eden]",
    "jvm.memory.pool.committed.bytes[name:g1_old_gen,pool:old]",
    "jvm.memory.pool.committed.bytes[name:g1_survivor_space,pool:survivor]",
    //JEP 197: Segmented Code Cache https://openjdk.java.net/jeps/197
    "jvm.memory.pool.committed.bytes[pool:codeheap_non_nmethods]",
    "jvm.memory.pool.committed.bytes[pool:codeheap_non_profiled_nmethods]",
    "jvm.memory.pool.committed.bytes[pool:codeheap_profiled_nmethods]",
    "jvm.memory.pool.committed.bytes[pool:compressed_class_space]",
    "jvm.memory.pool.committed.bytes[pool:metaspace]",
    "jvm.memory.pool.init.bytes[name:g1_eden_space,pool:eden]",
    "jvm.memory.pool.init.bytes[name:g1_old_gen,pool:old]",
    "jvm.memory.pool.init.bytes[name:g1_survivor_space,pool:survivor]",
    "jvm.memory.pool.init.bytes[pool:codeheap_non_nmethods]",
    "jvm.memory.pool.init.bytes[pool:codeheap_non_profiled_nmethods]",
    "jvm.memory.pool.init.bytes[pool:codeheap_profiled_nmethods]",
    "jvm.memory.pool.init.bytes[pool:compressed_class_space]",
    "jvm.memory.pool.init.bytes[pool:metaspace]",
    "jvm.memory.pool.max.bytes[name:g1_eden_space,pool:eden]",
    "jvm.memory.pool.max.bytes[name:g1_old_gen,pool:old]",
    "jvm.memory.pool.max.bytes[name:g1_survivor_space,pool:survivor]",
    "jvm.memory.pool.max.bytes[pool:codeheap_non_nmethods]",
    "jvm.memory.pool.max.bytes[pool:codeheap_non_profiled_nmethods]",
    "jvm.memory.pool.max.bytes[pool:codeheap_profiled_nmethods]",
    "jvm.memory.pool.max.bytes[pool:compressed_class_space]",
    "jvm.memory.pool.max.bytes[pool:metaspace]",
    "jvm.memory.pool.usage[name:g1_eden_space,pool:eden]",
    "jvm.memory.pool.usage[name:g1_old_gen,pool:old]",
    "jvm.memory.pool.usage[name:g1_survivor_space,pool:survivor]",
    "jvm.memory.pool.usage[pool:codeheap_non_nmethods]",
    "jvm.memory.pool.usage[pool:codeheap_non_profiled_nmethods]",
    "jvm.memory.pool.usage[pool:codeheap_profiled_nmethods]",
    "jvm.memory.pool.usage[pool:compressed_class_space]",
    "jvm.memory.pool.usage[pool:metaspace]",
    "jvm.memory.pool.used.bytes[name:g1_eden_space,pool:eden]",
    "jvm.memory.pool.used.bytes[name:g1_old_gen,pool:old]",
    "jvm.memory.pool.used.bytes[name:g1_survivor_space,pool:survivor]",
    "jvm.memory.pool.used.bytes[pool:codeheap_non_nmethods]",
    "jvm.memory.pool.used.bytes[pool:codeheap_non_profiled_nmethods]",
    "jvm.memory.pool.used.bytes[pool:codeheap_profiled_nmethods]",
    "jvm.memory.pool.used.bytes[pool:compressed_class_space]",
    "jvm.memory.pool.used.bytes[pool:metaspace]",
    "jvm.memory.pool.used_after_gc.bytes[name:g1_eden_space,pool:eden]",
    "jvm.memory.pool.used_after_gc.bytes[name:g1_old_gen,pool:old]",
    "jvm.memory.pool.used_after_gc.bytes[name:g1_survivor_space,pool:survivor]"
  };

  private MetricRegistry registry;

  @Before
  public void setUp() throws Exception {
    registry = new MetricRegistry();
    registry.registerAll(new JvmMetricSet());
  }

  @Test
  public void testJVMMetrics() throws Exception {

    boolean isJDK9Plus = false;
    try {
      Runtime.class.getMethod("version");
      isJDK9Plus = true;
    } catch (NoSuchMethodException e) {
      //Expected
    }

    String gc_type_old = isJDK9Plus ? "g1_old_generation" : "ps_marksweep";
    String gc_type_young = isJDK9Plus ? "g1_young_generation" : "ps_scavenge";
    List<String> expected = Arrays.asList(
            JVM_INFO_METRIC_NAME,
            "jvm.buffer_pool.capacity.bytes[pool:direct]",
            "jvm.buffer_pool.capacity.bytes[pool:mapped]",
            "jvm.buffer_pool.count[pool:direct]",
            "jvm.buffer_pool.count[pool:mapped]",
            "jvm.buffer_pool.used.bytes[pool:direct]",
            "jvm.buffer_pool.used.bytes[pool:mapped]",
            "jvm.classes.loaded",
            "jvm.classes.unloaded",
            "jvm.gc.count[generation:old,type:" + gc_type_old + "]",
            "jvm.gc.count[generation:young,type:" + gc_type_young + "]",
            "jvm.gc.time.seconds[generation:old,type:" + gc_type_old + "]",
            "jvm.gc.time.seconds[generation:young,type:" + gc_type_young + "]",
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

    if (isJDK9Plus){
      expectedMetrics.addAll(Arrays.asList(JDK11_MEM_METRICS));
    } else {
      expectedMetrics.addAll(Arrays.asList(JDK8_MEM_METRICS));
    }

    String metricsPackageVersion = MetricRegistry.class.getPackage().getImplementationVersion();
    if(!metricsPackageVersion.startsWith("3.")){
      expectedMetrics.add("jvm.threads.peak.count");
      expectedMetrics.add("jvm.threads.total_started.count");
    }

    try {
      // https://openjdk.java.net/jeps/352
      Class.forName("jdk.nio.mapmode.ExtendedMapMode");
      expectedMetrics.addAll(Arrays.asList(
        "jvm.buffer_pool.capacity.bytes[pool:mapped_non_volatile_memory]",
        "jvm.buffer_pool.count[pool:mapped_non_volatile_memory]",
        "jvm.buffer_pool.used.bytes[pool:mapped_non_volatile_memory]"
      ));
    } catch (ClassNotFoundException e) {
      //Expected pre JDK14
    }

    String osName = System.getProperty("os.name").toLowerCase();
    if (!osName.contains("windows")) {
      expectedMetrics.add("jvm.fd.max");
      expectedMetrics.add("jvm.fd.open");
    }

    assertEquals(expectedMetrics, registry.getNames());
  }
}