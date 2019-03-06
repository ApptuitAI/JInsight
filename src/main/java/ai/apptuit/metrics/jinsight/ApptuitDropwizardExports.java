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

import com.codahale.metrics.*;
import com.codahale.metrics.Timer;
import io.prometheus.client.dropwizard.samplebuilder.SampleBuilder;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ApptuitDropwizardExports extends io.prometheus.client.Collector
        implements io.prometheus.client.Collector.Describable {
  private static final Logger LOGGER = Logger.getLogger(ApptuitDropwizardExports.class.getName());
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

  private MetricFamilySamples fromSnapshotAndCount(String dropwizardName, Snapshot snapshot, long count, double factor, String helpMessage) {
    List<MetricFamilySamples.Sample> samples = Arrays.asList(
            sampleBuilder.createSample(dropwizardName, "_duration_min",
                    null, null, snapshot.getMin()),
            sampleBuilder.createSample(dropwizardName, "_duration_max",
                    null, null, snapshot.getMax()),
            sampleBuilder.createSample(dropwizardName, "_duration_mean",
                    null, null, snapshot.getMean()),
            sampleBuilder.createSample(dropwizardName, "_duration_stddev",
                    null, null, snapshot.getStdDev()),
            sampleBuilder.createSample(dropwizardName, "_duration",
                    Arrays.asList("quantile"), Arrays.asList("p50"), snapshot.getMedian() * factor),
            sampleBuilder.createSample(dropwizardName, "_duration",
                    Arrays.asList("quantile"), Arrays.asList("p75"), snapshot.get75thPercentile() * factor),
            sampleBuilder.createSample(dropwizardName, "_duration",
                    Arrays.asList("quantile"), Arrays.asList("p95"), snapshot.get95thPercentile() * factor),
            sampleBuilder.createSample(dropwizardName, "_duration",
                    Arrays.asList("quantile"), Arrays.asList("p98"), snapshot.get98thPercentile() * factor),
            sampleBuilder.createSample(dropwizardName, "_duration",
                    Arrays.asList("quantile"), Arrays.asList("p99"), snapshot.get99thPercentile() * factor),
            sampleBuilder.createSample(dropwizardName, "_duration",
                    Arrays.asList("quantile"), Arrays.asList("p999"), snapshot.get999thPercentile() * factor),
            sampleBuilder.createSample(dropwizardName, "_count",
                    new ArrayList<String>(), new ArrayList<String>(), count)
    );
    return new MetricFamilySamples(samples.get(0).name, Type.SUMMARY, helpMessage, samples);
  }

  private MetricFamilySamples fromHistogram(String dropwizardName, Histogram histogram) {
    return fromSnapshotAndCount(dropwizardName, histogram.getSnapshot(), histogram.getCount(), 1.0,
            getHelpMessage(dropwizardName, histogram));
  }

  private MetricFamilySamples fromCounter(String dropwizardName, Counter counter) {
    MetricFamilySamples.Sample sample = sampleBuilder.createSample(dropwizardName, "", new ArrayList<String>(), new ArrayList<String>(),
            Long.valueOf(counter.getCount()).doubleValue());
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
      LOGGER.log(Level.FINE, String.format("Invalid type for Gauge %s: %s", sanitizeMetricName(dropwizardName),
              obj == null ? "null" : obj.getClass().getName()));
      return null;
    }
    MetricFamilySamples.Sample sample = sampleBuilder.createSample(dropwizardName, "",
            new ArrayList<String>(), new ArrayList<String>(), value);
    return new MetricFamilySamples(sample.name, Type.GAUGE, getHelpMessage(dropwizardName, gauge), Arrays.asList(sample));
  }

  private MetricFamilySamples fromMeter(String dropwizardName, Meter meter) {
    List<MetricFamilySamples.Sample> samples = Arrays.asList(
            sampleBuilder.createSample(dropwizardName, "_rate",
                    Arrays.asList("window"), Arrays.asList("1min"), meter.getOneMinuteRate()),
            sampleBuilder.createSample(dropwizardName, "_rate",
                    Arrays.asList("window"), Arrays.asList("5min"), meter.getFiveMinuteRate()),
            sampleBuilder.createSample(dropwizardName, "_rate",
                    Arrays.asList("window"), Arrays.asList("15min"), meter.getFifteenMinuteRate())
    );
    return new MetricFamilySamples(samples.get(0).name, Type.SUMMARY, getHelpMessage(dropwizardName, meter), samples);
  }

  private MetricFamilySamples fromTimer(String dropwizardName, Timer timer) {
    return fromSnapshotAndCount(dropwizardName, timer.getSnapshot(), timer.getCount(),
            1.0D / TimeUnit.SECONDS.toNanos(1L), getHelpMessage(dropwizardName, timer));
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
