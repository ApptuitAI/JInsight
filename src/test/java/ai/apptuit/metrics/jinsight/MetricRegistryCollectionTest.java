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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.codahale.metrics.Clock;
import com.codahale.metrics.Counter;
import com.codahale.metrics.ExponentiallyDecayingReservoir;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metered;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SlidingTimeWindowArrayReservoir;
import com.codahale.metrics.SlidingTimeWindowReservoir;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import com.codahale.metrics.UniformReservoir;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Rajiv Shivane
 */
public class MetricRegistryCollectionTest {

  private MetricRegistry registry;

  private MetricRegistry registryCollection;

  @Before
  public void setUp() throws Exception {
    MetricRegistryCollection metricRegistryCollection = MetricRegistryCollection.getInstance();
    registryCollection = metricRegistryCollection.getRegistryCollection();
    registry = new MetricRegistry();
    metricRegistryCollection.register(registry);
  }

  @Test
  public void testRegister() throws Exception {
    MetricRegistryCollection metricRegistryCollection = MetricRegistryCollection.getInstance();
    MetricRegistry registry = new MetricRegistry();
    assertTrue(metricRegistryCollection.register(registry));
    assertFalse(metricRegistryCollection.register(registry));
  }

  @Test
  public void testDeRegister() throws Exception {
    MetricRegistryCollection metricRegistryCollection = MetricRegistryCollection.getInstance();
    MetricRegistry registry = new MetricRegistry();
    assertFalse(metricRegistryCollection.deRegister(registry));
    assertTrue(metricRegistryCollection.register(registry));
    assertTrue(metricRegistryCollection.deRegister(registry));
    assertFalse(metricRegistryCollection.deRegister(registry));
  }

  @Test
  public void testGauge() throws Exception {
    final String metricName = "test.gauge";
    AtomicLong gaugeValue = new AtomicLong();
    Gauge gauge = registry.gauge(metricName, () -> () -> gaugeValue);
    Gauge wrappedGauge = registryCollection.getGauges().get(metricName);
    assertNotEquals("Wrapped Metric is not expected to be same as original metric",
        gauge, wrappedGauge);

    assertEquals(gauge.getValue(), wrappedGauge.getValue());

    gaugeValue.set(7);
    assertEquals(gauge.getValue(), wrappedGauge.getValue());

    gaugeValue.set(3);
    assertEquals(gauge.getValue(), wrappedGauge.getValue());

    gaugeValue.set(1);
    assertEquals(gauge.getValue(), wrappedGauge.getValue());
  }

  @Test
  public void testCounter() throws Exception {
    final String metricName = "test.counter";
    Counter counter = registry.counter(metricName);
    Counter wrappedCounter = registryCollection.getCounters().get(metricName);
    assertNotEquals("Wrapped Metric is not expected to be same as original metric",
        counter, wrappedCounter);

    counter.inc();
    assertEquals(counter.getCount(), wrappedCounter.getCount());

    counter.inc(7);
    assertEquals(counter.getCount(), wrappedCounter.getCount());

    counter.dec(4);
    assertEquals(counter.getCount(), wrappedCounter.getCount());

    counter.dec();
    assertEquals(counter.getCount(), wrappedCounter.getCount());
  }


  @Test
  public void testHistogramDefault() throws Exception {
    final String metricName = "test.histogram.default";
    Histogram histogram = registry.histogram(metricName);
    Histogram wrappedHistogram = registryCollection.getHistograms().get(metricName);
    testHistogram(histogram, wrappedHistogram);
  }


  @Test
  public void testHistogramExponentiallyDecayingReservoir() throws Exception {
    final String metricName = "test.histogram.exp";
    Histogram histogram = registry.histogram(metricName, () -> new Histogram(new ExponentiallyDecayingReservoir()));
    Histogram wrappedHistogram = registryCollection.getHistograms().get(metricName);
    testHistogram(histogram, wrappedHistogram);
  }


  @Test
  public void testHistogramSlidingTimeWindowArrayReservoir() throws Exception {
    final String metricName = "test.histogram.stwa";
    Histogram histogram = registry.histogram(metricName,
        () -> new Histogram(new SlidingTimeWindowArrayReservoir(300, TimeUnit.SECONDS))
    );
    Histogram wrappedHistogram = registryCollection.getHistograms().get(metricName);
    testHistogram(histogram, wrappedHistogram);
  }


  @Test
  public void testHistogramSlidingTimeWindowReservoir() throws Exception {
    final String metricName = "test.histogram.stw";
    Histogram histogram = registry.histogram(metricName,
        () -> new Histogram(new SlidingTimeWindowReservoir(300, TimeUnit.SECONDS))
    );
    Histogram wrappedHistogram = registryCollection.getHistograms().get(metricName);
    testHistogram(histogram, wrappedHistogram);
  }

  @Test
  public void testHistogramUniformReservoir() throws Exception {
    final String metricName = "test.histogram.uniform";
    Histogram histogram = registry.histogram(metricName,
        () -> new Histogram(new UniformReservoir())
    );
    Histogram wrappedHistogram = registryCollection.getHistograms().get(metricName);
    testHistogram(histogram, wrappedHistogram);
  }

  private void testHistogram(Histogram histogram, Histogram wrappedHistogram) {
    assertNotEquals("Wrapped Metric is not expected to be same as original metric",
        histogram, wrappedHistogram);

    assertEquals(histogram.getCount(), wrappedHistogram.getCount());

    histogram.update(1);
    assertEquals(histogram.getCount(), wrappedHistogram.getCount());
    assertSnapshotEquals(histogram.getSnapshot(), wrappedHistogram.getSnapshot());

    histogram.update(3);
    histogram.update(5);
    histogram.update(7);
    histogram.update(8);
    histogram.update(8);
    histogram.update(9);
    histogram.update(9);
    histogram.update(9);
    histogram.update(11);
    assertEquals(histogram.getCount(), wrappedHistogram.getCount());
    assertSnapshotEquals(histogram.getSnapshot(), wrappedHistogram.getSnapshot());
  }

  private void assertSnapshotEquals(Snapshot snapshot, Snapshot wrappedSnapshot) {
    assertEquals(snapshot.size(), wrappedSnapshot.size());
    assertEquals(snapshot.getMax(), wrappedSnapshot.getMax());
    assertEquals(snapshot.getMin(), wrappedSnapshot.getMin());
    assertEquals(snapshot.getMean(), wrappedSnapshot.getMean(), 0.001);
    assertEquals(snapshot.getMedian(), wrappedSnapshot.getMedian(), 0.001);
    assertEquals(snapshot.getStdDev(), wrappedSnapshot.getStdDev(), 0.001);
    assertEquals(snapshot.get75thPercentile(), wrappedSnapshot.get75thPercentile(), 0.001);
    assertEquals(snapshot.get95thPercentile(), wrappedSnapshot.get95thPercentile(), 0.001);
    assertEquals(snapshot.get98thPercentile(), wrappedSnapshot.get98thPercentile(), 0.001);
    assertEquals(snapshot.get99thPercentile(), wrappedSnapshot.get99thPercentile(), 0.001);
    assertEquals(snapshot.get999thPercentile(), wrappedSnapshot.get999thPercentile(), 0.001);
    assertEquals(snapshot.getValue(0.05), wrappedSnapshot.getValue(0.05), 0.001);
    assertEquals(snapshot.getValue(0.001), wrappedSnapshot.getValue(0.001), 0.001);
    assertArrayEquals(snapshot.getValues(), wrappedSnapshot.getValues());

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    wrappedSnapshot.dump(output);

    if (snapshot.size() > 0) {
      assertThat(new String(output.toByteArray()), CoreMatchers.not(""));
    }
  }

  @Test
  public void testMeter() throws Exception {
    final String metricName = "test.meter";
    final AdjustableClock clock = new AdjustableClock();
    Meter meter = registry.meter(metricName, () -> new Meter(clock));
    Meter wrappedMeter = registryCollection.getMeters().get(metricName);
    assertNotEquals("Wrapped Metric is not expected to be same as original metric",
        meter, wrappedMeter);

    assertMeterEquals(meter, wrappedMeter);

    for (int i = 0; i < 20; i++) {
      clock.setDeltaSeconds(i * 60);
      meter.mark(1000);
    }
    assertMeterEquals(meter, wrappedMeter);
  }

  private void assertMeterEquals(Metered meter, Metered wrappedMeter) {
    assertEquals(meter.getCount(), wrappedMeter.getCount());
    assertEquals(meter.getMeanRate(), wrappedMeter.getMeanRate(), 0.001);
    assertEquals(meter.getOneMinuteRate(), wrappedMeter.getOneMinuteRate(), 0.001);
    assertEquals(meter.getFiveMinuteRate(), wrappedMeter.getFiveMinuteRate(), 0.001);
    assertEquals(meter.getFifteenMinuteRate(), wrappedMeter.getFifteenMinuteRate(), 0.001);
  }


  @Test
  public void testTimerExp() throws Exception {
    final String metricName = "test.timer.exp";
    final AdjustableClock clock = new AdjustableClock();
    Timer timer = registry.timer(metricName,
        () -> new Timer(new ExponentiallyDecayingReservoir(), clock));
    Timer wrappedTimer = registryCollection.getTimers().get(metricName);
    assertNotEquals("Wrapped Metric is not expected to be same as original metric",
        timer, wrappedTimer);

    assertMeterEquals(timer, wrappedTimer);
    assertSnapshotEquals(timer.getSnapshot(), wrappedTimer.getSnapshot());

    for (int i = 0; i < 20; i++) {
      clock.setDeltaSeconds(i * 60);
      for (int j = 0; j < 2000; j++) {
        int duration = (int) ((Math.random() + 1) * 100);
        timer.update(duration, TimeUnit.MILLISECONDS);
      }
    }
    assertMeterEquals(timer, wrappedTimer);
    assertSnapshotEquals(timer.getSnapshot(), wrappedTimer.getSnapshot());
  }


  private static class AdjustableClock extends Clock {

    private Clock delegate = Clock.defaultClock();
    private long delta = 0;

    @Override
    public long getTick() {
      return delegate.getTick() + delta;
    }

    @Override
    public long getTime() {
      return getTick() / (1000 * 1000);
    }

    public void setDeltaSeconds(long delta) {
      this.delta = delta * (1000 * 1000 * 1000);
    }
  }
}
