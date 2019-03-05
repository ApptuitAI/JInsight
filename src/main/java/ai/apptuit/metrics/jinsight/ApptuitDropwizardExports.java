package ai.apptuit.metrics.jinsight;

import com.codahale.metrics.*;
import com.codahale.metrics.Timer;
import io.prometheus.client.dropwizard.DropwizardExports;
import io.prometheus.client.dropwizard.samplebuilder.SampleBuilder;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class ApptuitDropwizardExports extends  DropwizardExports {
  private static final Logger LOGGER = Logger.getLogger(ApptuitDropwizardExports.class.getName());
  private MetricRegistry registry;
  private SampleBuilder sampleBuilder;

  public ApptuitDropwizardExports(MetricRegistry registry, SampleBuilder builder) {
    super(registry, builder);
    this.registry = registry;
    this.sampleBuilder = builder;
  }

  private static String getHelpMessage(String metricName, Metric metric) {
    return String.format("Generated from Dropwizard metric import (metric=%s, type=%s)",
            metricName, metric.getClass().getName());
  }

  MetricFamilySamples fromSnapshotAndCount(String dropwizardName, Snapshot snapshot, long count, double factor, String helpMessage) {
    List<MetricFamilySamples.Sample> samples = Arrays.asList(
            sampleBuilder.createSample(dropwizardName, "duration_min", null, null, snapshot.getMin()),
            sampleBuilder.createSample(dropwizardName, "duration_max", null, null, snapshot.getMax()),
            sampleBuilder.createSample(dropwizardName, "duration_mean", null, null, snapshot.getMean()),
            sampleBuilder.createSample(dropwizardName, "duration_stddev", null, null, snapshot.getStdDev()),
            sampleBuilder.createSample(dropwizardName, "duration", Arrays.asList("quantile"), Arrays.asList("p50"), snapshot.getMedian() * factor),
            sampleBuilder.createSample(dropwizardName, "duration", Arrays.asList("quantile"), Arrays.asList("p75"), snapshot.get75thPercentile() * factor),
            sampleBuilder.createSample(dropwizardName, "duration", Arrays.asList("quantile"), Arrays.asList("p95"), snapshot.get95thPercentile() * factor),
            sampleBuilder.createSample(dropwizardName, "duration", Arrays.asList("quantile"), Arrays.asList("p98"), snapshot.get98thPercentile() * factor),
            sampleBuilder.createSample(dropwizardName, "duration", Arrays.asList("quantile"), Arrays.asList("p99"), snapshot.get99thPercentile() * factor),
            sampleBuilder.createSample(dropwizardName, "duration", Arrays.asList("quantile"), Arrays.asList("p999"), snapshot.get999thPercentile() * factor),
            sampleBuilder.createSample(dropwizardName, "_count", new ArrayList<String>(), new ArrayList<String>(), count)
    );
    return new MetricFamilySamples(samples.get(0).name, Type.SUMMARY, helpMessage, samples);
  }

  private MetricFamilySamples fromHistogram(String dropwizardName, Histogram histogram) {
    return fromSnapshotAndCount(dropwizardName, histogram.getSnapshot(), histogram.getCount(), 1.0,
            getHelpMessage(dropwizardName, histogram));
  }

  private MetricFamilySamples fromMeter(String dropwizardName, Meter meter) {
    List<MetricFamilySamples.Sample> samples = Arrays.asList(
            sampleBuilder.createSample(dropwizardName, "rate", Arrays.asList("window"), Arrays.asList("1min"), meter.getOneMinuteRate()),
            sampleBuilder.createSample(dropwizardName, "rate", Arrays.asList("window"), Arrays.asList("5min"), meter.getFiveMinuteRate()),
            sampleBuilder.createSample(dropwizardName, "rate", Arrays.asList("window"), Arrays.asList("15min"), meter.getFifteenMinuteRate())
    );
    return new MetricFamilySamples(samples.get(0).name, Type.SUMMARY, getHelpMessage(dropwizardName, meter), samples);
  }

  private MetricFamilySamples fromTimer(String dropwizardName, Timer timer) {
    return fromSnapshotAndCount(dropwizardName, timer.getSnapshot(), timer.getCount(),
            1.0D / TimeUnit.SECONDS.toNanos(1L), getHelpMessage(dropwizardName, timer));
  }

  @Override
  public List<MetricFamilySamples> collect() {
    Map<String, MetricFamilySamples> mfSamplesMap = new HashMap<String, MetricFamilySamples>();

    for (SortedMap.Entry<String, Histogram> entry : registry.getHistograms().entrySet()) {
      addToMap(mfSamplesMap, fromHistogram(entry.getKey(), entry.getValue()));
    }
    for (SortedMap.Entry<String, Timer> entry : registry.getTimers().entrySet()) {
      addToMap(mfSamplesMap, fromTimer(entry.getKey(), entry.getValue()));
    }
    for (SortedMap.Entry<String, Meter> entry : registry.getMeters().entrySet()) {
      addToMap(mfSamplesMap, fromMeter(entry.getKey(), entry.getValue()));
    }
    registry.removeMatching(CustomMetricFilter.FILTER);
    ArrayList<MetricFamilySamples> samplesList = new ArrayList<>(mfSamplesMap.values());
    samplesList.addAll(super.collect());
    return samplesList;
  }

  private void addToMap(Map<String, MetricFamilySamples> mfSamplesMap, MetricFamilySamples newMfSamples) {
    if (newMfSamples != null) {
      MetricFamilySamples currentMfSamples = mfSamplesMap.get(newMfSamples.name);
      if (currentMfSamples == null) {
        mfSamplesMap.put(newMfSamples.name, newMfSamples);
      } else {
        List<MetricFamilySamples.Sample> samples = new ArrayList<MetricFamilySamples.Sample>(currentMfSamples.samples);
        samples.addAll(newMfSamples.samples);
        mfSamplesMap.put(newMfSamples.name, new MetricFamilySamples(newMfSamples.name, currentMfSamples.type, currentMfSamples.help, samples));
      }
    }
  }

  public interface CustomMetricFilter extends com.codahale.metrics.MetricFilter {
    CustomMetricFilter FILTER = new CustomMetricFilter() {
      @Override
      public boolean matches(String name, Metric metric) {
        return (metric.getClass() == Histogram.class || metric.getClass() == Timer.class || metric.getClass() == Meter.class);
      }
    };
  }
}
