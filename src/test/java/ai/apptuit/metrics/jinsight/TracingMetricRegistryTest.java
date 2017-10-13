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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.codahale.metrics.Counter;
import com.codahale.metrics.ExponentiallyDecayingReservoir;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Rajiv Shivane
 */

public class TracingMetricRegistryTest {

  private Metric updatedMetric = null;
  private MetricRegistry registry;

  @Before
  public void setUp() throws Exception {

    registry = new TracingMetricRegistry() {
      @Override
      protected void onUpdate(TraceableMetric metric) {
        assertNull(updatedMetric);
        updatedMetric = metric;
        super.onUpdate(metric);
      }
    };

  }

  @After
  public void tearDown() throws Exception {
    updatedMetric = null;
  }

  @Test
  public void testTimerUpdate() throws Exception {
    Timer a = registry.timer("A");
    a.update(1, TimeUnit.MILLISECONDS);
    assertEquals(a, updatedMetric);
  }

  @Test
  public void testTimerTime() throws Exception {
    Timer a = registry.timer("B");
    Context context = a.time();
    context.stop();
    assertEquals(a, updatedMetric);
  }

  @Test
  public void testCounterInc() throws Exception {
    Counter counter = registry.counter("C1");
    counter.inc();
    assertEquals(counter, updatedMetric);
  }

  @Test
  public void testCounterIncInt() throws Exception {
    Counter counter = registry.counter("C2");
    counter.inc(10);
    assertEquals(counter, updatedMetric);
  }

  @Test
  public void testCounterDec() throws Exception {
    Counter counter = registry.counter("C3");
    counter.dec();
    assertEquals(counter, updatedMetric);
  }

  @Test
  public void testCounterDecInt() throws Exception {
    Counter counter = registry.counter("C4");
    counter.dec(10);
    assertEquals(counter, updatedMetric);
  }

  @Test
  public void testMeter() throws Exception {
    Meter m = registry.meter("M1");
    m.mark();
    assertEquals(m, updatedMetric);
  }

  @Test
  public void testMeterInt() throws Exception {
    Meter m = registry.meter("M2");
    m.mark(10);
    assertEquals(m, updatedMetric);
  }

  @Test
  public void testHistorgramInt() throws Exception {
    Histogram h = registry.histogram("H1");
    h.update(1);
    assertEquals(h, updatedMetric);
  }

  @Test
  public void testHistorgramLong() throws Exception {
    Histogram h = registry.histogram("H2");
    h.update(1L);
    assertEquals(h, updatedMetric);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testCounterSupplier() throws Exception {
    registry.counter("CXX", Counter::new);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testMeterSupplier() throws Exception {
    registry.meter("MXX", Meter::new);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testTimerSupplier() throws Exception {
    registry.timer("TXX", Timer::new);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testHistogramSupplier() throws Exception {
    registry.histogram("HXX", () -> new Histogram(new ExponentiallyDecayingReservoir()));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testGaugeSupplier() throws Exception {
    registry.gauge("GXX", () -> () -> 7);
  }
}
