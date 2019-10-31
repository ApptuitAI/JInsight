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

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.dropwizard.samplebuilder.SampleBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ApptuitDropwizardExportsTests {

  private CollectorRegistry registry = new CollectorRegistry();
  private MetricRegistry metricRegistry;
  private SampleBuilder sampleBuilder;
  private Map<String, String> globalTags;

  @Before
  public void setUp() throws Exception {
    metricRegistry = new MetricRegistry();
    globalTags = new HashMap<>();
    globalTags.put("host", "JIhost");
    globalTags.put("testTag1", "testValue1");
    globalTags.put("testTag2", "testValue2");
    sampleBuilder = Mockito.mock(TagDecodingSampleBuilder.class);
    new ApptuitDropwizardExports(metricRegistry, sampleBuilder).register(registry);
  }

  @Test
  public void testHistogram() throws Exception {
    // just test the standard mapper
    final MetricRegistry metricRegistry = new MetricRegistry();
    final CollectorRegistry registry = new CollectorRegistry();
    Histogram hist = metricRegistry.histogram("hist");
    ApptuitDropwizardExports exporter = new ApptuitDropwizardExports(metricRegistry, new TagDecodingSampleBuilder(null)).register(registry);
    int i = 0;
    while (i < 100) {
      hist.update(i);
      i += 1;
    }
    Snapshot snapshot = hist.getSnapshot();
    Collector.MetricFamilySamples.Sample[] samples = {
            new Collector.MetricFamilySamples.Sample("hist_min", Collections.<String>emptyList(), Collections.<String>emptyList(), snapshot.getMin()),
            new Collector.MetricFamilySamples.Sample("hist_max", Collections.<String>emptyList(), Collections.<String>emptyList(), snapshot.getMax()),
            new Collector.MetricFamilySamples.Sample("hist_mean", Collections.<String>emptyList(), Collections.<String>emptyList(), snapshot.getMean()),
            new Collector.MetricFamilySamples.Sample("hist_stddev", Collections.<String>emptyList(), Collections.<String>emptyList(), snapshot.getStdDev()),
            new Collector.MetricFamilySamples.Sample("hist_count", Collections.<String>emptyList(), Collections.<String>emptyList(), hist.getCount()),
    };
    Collector.MetricFamilySamples samplesFromExporter = exporter.collect().get(0);
    for (Collector.MetricFamilySamples.Sample sample : samples) {
      assertTrue(samplesFromExporter.samples.contains(sample));
    }
  }

  @Test
  public void testMeter() throws Exception {
    // just test the standard mapper
    final MetricRegistry metricRegistry = new MetricRegistry();
    final CollectorRegistry registry = new CollectorRegistry();
    Meter meter = metricRegistry.meter("meter");
    ApptuitDropwizardExports exporter = new ApptuitDropwizardExports(metricRegistry, new TagDecodingSampleBuilder(null)).register(registry);
    int i = 0;
    while (i < 100) {
      meter.mark();
      i += 1;
    }
    Collector.MetricFamilySamples.Sample[] samples = {
            new Collector.MetricFamilySamples.Sample("meter_rate", Arrays.asList("window"), Arrays.asList("1m"), meter.getOneMinuteRate()),
            new Collector.MetricFamilySamples.Sample("meter_rate", Arrays.asList("window"), Arrays.asList("5m"), meter.getFiveMinuteRate()),
            new Collector.MetricFamilySamples.Sample("meter_rate", Arrays.asList("window"), Arrays.asList("15m"), meter.getFifteenMinuteRate()),
            new Collector.MetricFamilySamples.Sample("meter_total", new ArrayList<>(), new ArrayList<>(), meter.getCount())
    };
    Collector.MetricFamilySamples samplesFromExporter = exporter.collect().get(0);
    for (Collector.MetricFamilySamples.Sample sample : samples) {
      assertTrue(samplesFromExporter.samples.contains(sample));
    }
  }

  @Test
  public void testGauge() {
    Gauge<Integer> integerGauge = new Gauge<Integer>() {
      @Override
      public Integer getValue() {
        return 1234;
      }
    };
    Gauge<Double> doubleGauge = new Gauge<Double>() {
      @Override
      public Double getValue() {
        return 1.234D;
      }
    };
    Gauge<Long> longGauge = new Gauge<Long>() {
      @Override
      public Long getValue() {
        return 1234L;
      }
    };
    Gauge<Float> floatGauge = new Gauge<Float>() {
      @Override
      public Float getValue() {
        return 0.1234F;
      }
    };
    Gauge<Boolean> booleanGauge = new Gauge<Boolean>() {
      @Override
      public Boolean getValue() {
        return true;
      }
    };

    Mockito.when(sampleBuilder.createSample("integer.gauge", "", Collections.emptyList(), Collections.emptyList(), 1234)).thenReturn(new Collector.MetricFamilySamples.Sample("integer_gauge", Collections.<String>emptyList(), Collections.<String>emptyList(), 1234));
    Mockito.when(sampleBuilder.createSample("long.gauge", "", Collections.emptyList(), Collections.emptyList(), 1234)).thenReturn(new Collector.MetricFamilySamples.Sample("long_gauge", Collections.<String>emptyList(), Collections.<String>emptyList(), 1234));
    Mockito.when(sampleBuilder.createSample("double.gauge", "", Collections.emptyList(), Collections.emptyList(), 1.234)).thenReturn(new Collector.MetricFamilySamples.Sample("double_gauge", Collections.<String>emptyList(), Collections.<String>emptyList(), 1.234));
    Mockito.when(sampleBuilder.createSample("float.gauge", "", Collections.emptyList(), Collections.emptyList(), 0.1234F)).thenReturn(new Collector.MetricFamilySamples.Sample("float_gauge", Collections.<String>emptyList(), Collections.<String>emptyList(), 0.1234F));
    Mockito.when(sampleBuilder.createSample("boolean.gauge", "", Collections.emptyList(), Collections.emptyList(), 1)).thenReturn(new Collector.MetricFamilySamples.Sample("boolean_gauge", Collections.<String>emptyList(), Collections.<String>emptyList(), 1));

    metricRegistry.register("double.gauge", doubleGauge);
    metricRegistry.register("long.gauge", longGauge);
    metricRegistry.register("integer.gauge", integerGauge);
    metricRegistry.register("float.gauge", floatGauge);
    metricRegistry.register("boolean.gauge", booleanGauge);

    assertEquals(new Double(1234),
            registry.getSampleValue("integer_gauge", new String[]{}, new String[]{}));
    assertEquals(new Double(1234),
            registry.getSampleValue("long_gauge", new String[]{}, new String[]{}));
    assertEquals(new Double(1.234),
            registry.getSampleValue("double_gauge", new String[]{}, new String[]{}));
    assertEquals(new Double(0.1234F),
            registry.getSampleValue("float_gauge", new String[]{}, new String[]{}));
    assertEquals(new Double(1),
            registry.getSampleValue("boolean_gauge", new String[]{}, new String[]{}));
  }

  @Test
  public void testTimer() throws IOException, InterruptedException {
    // just test the standard mapper
    final MetricRegistry metricRegistry = new MetricRegistry();
    final CollectorRegistry registry = new CollectorRegistry();
    ApptuitDropwizardExports exporter = new ApptuitDropwizardExports(metricRegistry, new TagDecodingSampleBuilder(null)).register(registry);

    Timer t = metricRegistry.timer("timer");
    t.update(1, TimeUnit.MILLISECONDS);
    List<Collector.MetricFamilySamples.Sample> samplesFromExporter = exporter.collect().get(0).samples;

    assertTrue(samplesFromExporter.get(samplesFromExporter.size() - 2).value >= 0.001);
    //count
    assertEquals(0, Double.compare(1.0D, samplesFromExporter.get(samplesFromExporter.size() - 1).value));
  }

  @Test
  public void testBrokenMetrics() {
    MetricRegistry metricRegistry = new MetricRegistry();
    CollectorRegistry registry = new CollectorRegistry();
    ApptuitDropwizardExports exporter = new ApptuitDropwizardExports(metricRegistry, new TagDecodingSampleBuilder(null));
    exporter.register(registry);

    metricRegistry.register("good.gauge", (Gauge<Integer>) () -> 1234);
    metricRegistry.register("bad.gauge", (Gauge<Integer>) () -> {
      throw new RuntimeException();
    });

    assertEquals(new Double(1234),
        registry.getSampleValue("good_gauge", new String[]{}, new String[]{}));
    assertNull(registry.getSampleValue("bad_gauge", new String[]{}, new String[]{}));
  }

  @Test
  public void counterTest() throws Exception {
    Mockito.when(sampleBuilder.createSample("foo.bar", "", Collections.<String>emptyList(), Collections.<String>emptyList(), 1d)).thenReturn(new Collector.MetricFamilySamples.Sample("foo_bar", Collections.<String>emptyList(), Collections.<String>emptyList(), 1d));
    metricRegistry.counter("foo.bar").inc();
    assertEquals(new Double(1),
            registry.getSampleValue("foo_bar")
    );
  }

}
