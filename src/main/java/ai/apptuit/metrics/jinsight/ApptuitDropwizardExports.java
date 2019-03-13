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

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metered;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Sampling;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;

import io.prometheus.client.dropwizard.samplebuilder.SampleBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ApptuitDropwizardExports extends io.prometheus.client.Collector
        implements io.prometheus.client.Collector.Describable {
  private static final Logger LOGGER = Logger.getLogger(ApptuitDropwizardExports.class.getName());

  private static final String QUANTILE_TAG_NAME = "quantile";
  private static final String WINDOW_TAG_NAME = "window";
  private static final String RATE_SUFFIX = "_rate";

  private MetricRegistry registry;
  private SampleBuilder sampleBuilder;

  public ApptuitDropwizardExports(MetricRegistry registry, SampleBuilder builder) {
    this.registry = registry;
    this.sampleBuilder = builder;

  }

  private static String getHelpMessage(String metricName, Metric metric) {
    return String.format("Generated from Dropwizard metric import (metric=%s, type=%s)",
            metricName, metric.getClass().getName());
  }

  private MetricFamilySamples fromSnapshotAndCount(String dropwizardName, String durationSuffix, Sampling samplingObj, long count, double factor, String helpMessage) {
    Snapshot snapshot = samplingObj.getSnapshot();
    List<MetricFamilySamples.Sample> samples = Arrays.asList(
            sampleBuilder.createSample(dropwizardName, durationSuffix + "_min",
                    null, null, snapshot.getMin()),
            sampleBuilder.createSample(dropwizardName, durationSuffix + "_max",
                    null, null, snapshot.getMax()),
            sampleBuilder.createSample(dropwizardName, durationSuffix + "_mean",
                    null, null, snapshot.getMean()),
            sampleBuilder.createSample(dropwizardName, durationSuffix + "_stddev",

                    null, null, snapshot.getStdDev()),
            sampleBuilder.createSample(dropwizardName, durationSuffix,
                    Arrays.asList(QUANTILE_TAG_NAME), Arrays.asList("0.5"), snapshot.getMedian() * factor),
            sampleBuilder.createSample(dropwizardName, durationSuffix,
                    Arrays.asList(QUANTILE_TAG_NAME), Arrays.asList("0.75"), snapshot.get75thPercentile() * factor),
            sampleBuilder.createSample(dropwizardName, durationSuffix,
                    Arrays.asList(QUANTILE_TAG_NAME), Arrays.asList("0.95"), snapshot.get95thPercentile() * factor),
            sampleBuilder.createSample(dropwizardName, durationSuffix,
                    Arrays.asList(QUANTILE_TAG_NAME), Arrays.asList("0.98"), snapshot.get98thPercentile() * factor),
            sampleBuilder.createSample(dropwizardName, durationSuffix,
                    Arrays.asList(QUANTILE_TAG_NAME), Arrays.asList("0.99"), snapshot.get99thPercentile() * factor),
            sampleBuilder.createSample(dropwizardName, durationSuffix,
                    Arrays.asList(QUANTILE_TAG_NAME), Arrays.asList("0.999"), snapshot.get999thPercentile() * factor),
            sampleBuilder.createSample(dropwizardName, "_count",
                    new ArrayList<>(), new ArrayList<>(), count)
    );
    return new MetricFamilySamples(samples.get(0).name, Type.SUMMARY, helpMessage, samples);
  }

  private MetricFamilySamples fromHistogram(String dropwizardName, Histogram histogram) {
    return fromSnapshotAndCount(dropwizardName, "", histogram, histogram.getCount(), 1.0,
            getHelpMessage(dropwizardName, histogram));
  }

  private MetricFamilySamples fromCounter(String dropwizardName, Counter counter) {
    MetricFamilySamples.Sample sample = sampleBuilder.createSample(dropwizardName, "", new ArrayList<>(), new ArrayList<>(),
            counter.getCount());
    return new MetricFamilySamples(sample.name, Type.GAUGE, getHelpMessage(dropwizardName, counter), Arrays.asList(sample));
  }

  /**
   * Export gauge as a prometheus gauge.
   */
  private MetricFamilySamples fromGauge(String dropwizardName, Gauge gauge) {
    Object obj = gauge.getValue();
    double value;
    if (obj instanceof Number) {
      value = ((Number) obj).doubleValue();
    } else if (obj instanceof Boolean) {
      value = ((Boolean) obj) ? 1 : 0;
    } else {
      String objName = "null";
      if (obj != null) {
        objName = obj.getClass().getName();
      }
      LOGGER.log(Level.FINE, String.format("Invalid type for Gauge %s: %s", sanitizeMetricName(dropwizardName),
              objName));
      return null;
    }
    MetricFamilySamples.Sample sample = sampleBuilder.createSample(dropwizardName, "",
            new ArrayList<>(), new ArrayList<>(), value);
    return new MetricFamilySamples(sample.name, Type.GAUGE, getHelpMessage(dropwizardName, gauge), Arrays.asList(sample));
  }

  private MetricFamilySamples fromMeter(String dropwizardName, Metered meter) {
    List<MetricFamilySamples.Sample> samples = Arrays.asList(
            sampleBuilder.createSample(dropwizardName, RATE_SUFFIX,
                    Arrays.asList(WINDOW_TAG_NAME), Arrays.asList("1m"), meter.getOneMinuteRate()),
            sampleBuilder.createSample(dropwizardName, RATE_SUFFIX,
                    Arrays.asList(WINDOW_TAG_NAME), Arrays.asList("5m"), meter.getFiveMinuteRate()),
            sampleBuilder.createSample(dropwizardName, RATE_SUFFIX,
                    Arrays.asList(WINDOW_TAG_NAME), Arrays.asList("15m"), meter.getFifteenMinuteRate()),
            sampleBuilder.createSample(dropwizardName, "_total", new ArrayList<String>(), new ArrayList<String>(), meter.getCount())
    );
    return new MetricFamilySamples(samples.get(0).name, Type.SUMMARY, getHelpMessage(dropwizardName, meter), samples);
  }

  private MetricFamilySamples fromTimer(String dropwizardName, Timer timer) {
    List<MetricFamilySamples.Sample> samples = new ArrayList<>();
    samples.addAll(fromMeter(dropwizardName, timer).samples);
    samples.remove(samples.size() - 1);
    samples.addAll(fromSnapshotAndCount(dropwizardName, "_duration", timer, timer.getCount(),
            1.0D / TimeUnit.SECONDS.toNanos(1L), getHelpMessage(dropwizardName, timer)).samples);

    return new MetricFamilySamples(samples.get(0).name, Type.SUMMARY, getHelpMessage(dropwizardName, timer), samples);
  }

  @Override
  public List<MetricFamilySamples> collect() {

    Map<String, MetricFamilySamples> mfSamplesMap = new HashMap<>();

    for (SortedMap.Entry<String, Histogram> entry : registry.getHistograms().entrySet()) {
      addToMap(mfSamplesMap, fromHistogram(entry.getKey(), entry.getValue()));
    }
    for (SortedMap.Entry<String, Timer> entry : registry.getTimers().entrySet()) {
      addToMap(mfSamplesMap, fromTimer(entry.getKey(), entry.getValue()));
    }
    for (SortedMap.Entry<String, Meter> entry : registry.getMeters().entrySet()) {
      addToMap(mfSamplesMap, fromMeter(entry.getKey(), entry.getValue()));
    }
    for (SortedMap.Entry<String, Gauge> entry : registry.getGauges().entrySet()) {
      addToMap(mfSamplesMap, fromGauge(entry.getKey(), entry.getValue()));
    }
    for (SortedMap.Entry<String, Counter> entry : registry.getCounters().entrySet()) {
      addToMap(mfSamplesMap, fromCounter(entry.getKey(), entry.getValue()));
    }
    return new ArrayList<>(mfSamplesMap.values());
  }

  private void addToMap(Map<String, MetricFamilySamples> mfSamplesMap, MetricFamilySamples newMfSamples) {
    if (newMfSamples != null) {
      MetricFamilySamples currentMfSamples = mfSamplesMap.get(newMfSamples.name);
      if (currentMfSamples == null) {
        mfSamplesMap.put(newMfSamples.name, newMfSamples);
      } else {
        List<MetricFamilySamples.Sample> samples = new ArrayList<>(currentMfSamples.samples);
        samples.addAll(newMfSamples.samples);
        mfSamplesMap.put(newMfSamples.name,
                new MetricFamilySamples(newMfSamples.name, currentMfSamples.type, currentMfSamples.help, samples));
      }
    }
  }

  @Override
  public List<MetricFamilySamples> describe() {
    return new ArrayList<>();
  }
}
