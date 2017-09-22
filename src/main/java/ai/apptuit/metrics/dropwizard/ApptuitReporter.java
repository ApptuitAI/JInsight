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

package ai.apptuit.metrics.dropwizard;

import ai.apptuit.metrics.client.ApptuitPutClient;
import ai.apptuit.metrics.client.DataPoint;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metered;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

/**
 * @author Rajiv Shivane
 */
public class ApptuitReporter extends ScheduledReporter {

  public enum ReportingMode {
    NO_OP, SYS_OUT, API_PUT
  }

  private static final boolean DEBUG = false;
  private static final ReportingMode REPORTING_MODE = ReportingMode.API_PUT;

  private static final String REPORTER_NAME = "apptuit-reporter";

  private final Timer buildReportTimer;
  private final Timer sendReportTimer;
  private final DataPointsReporter dataPointsReporter;


  protected ApptuitReporter(MetricRegistry registry, MetricFilter filter, TimeUnit rateUnit,
      TimeUnit durationUnit,
      Map<String, String> globalTags, String key) {
    super(registry, REPORTER_NAME, filter, rateUnit, durationUnit);

    this.buildReportTimer = registry.timer("apptuit.reporter.report.build");
    this.sendReportTimer = registry.timer("apptuit.reporter.report.send");

    switch (REPORTING_MODE) {
      case NO_OP:
        this.dataPointsReporter = dataPoints -> {
        };
        break;
      case SYS_OUT:
        this.dataPointsReporter = dataPoints -> {
          dataPoints.forEach(dp -> dp.toTextLine(System.out, globalTags));
        };
        break;
      case API_PUT:
      default:
        ApptuitPutClient putClient = new ApptuitPutClient(key, globalTags);
        this.dataPointsReporter = putClient::put;
    }
  }

  @Override
  public void report(SortedMap<String, Gauge> gauges, SortedMap<String, Counter> counters,
      SortedMap<String, Histogram> histograms, SortedMap<String, Meter> meters,
      SortedMap<String, Timer> timers) {

    DataPointCollector collector = new DataPointCollector(System.currentTimeMillis() / 1000);

    buildReportTimer.time(() -> {
      debug("################");

      debug(">>>>>>>> Guages <<<<<<<<<");
      gauges.forEach(collector::collectGauge);
      debug(">>>>>>>> Counters <<<<<<<<<");
      counters.forEach(collector::collectCounter);
      debug(">>>>>>>> Histograms <<<<<<<<<");
      histograms.forEach(collector::collectHistogram);
      debug(">>>>>>>> Meters <<<<<<<<<");
      meters.forEach(collector::collectMeter);
      debug(">>>>>>>> Timers <<<<<<<<<");
      timers.forEach(collector::collectTimer);

      debug("################");
    });

    sendReportTimer.time(() -> {
      Collection<DataPoint> dataPoints = collector.dataPoints;
      dataPointsReporter.put(dataPoints);
      //dataPoints.forEach(System.out::println);
    });
  }

  private void debug(Object s) {
    if (DEBUG) {
      System.out.println(s);
    }
  }

  private class DataPointCollector {

    private final long epoch;
    private final List<DataPoint> dataPoints;

    DataPointCollector(long epoch) {
      this.epoch = epoch;
      this.dataPoints = new LinkedList<>();
    }

    private void collectGauge(String name, Gauge gauge) {
      Object value = gauge.getValue();
      if (value instanceof BigDecimal) {
        addDataPoint(name, ((BigDecimal) value).doubleValue());
      } else if (value instanceof BigInteger) {
        addDataPoint(name, ((BigInteger) value).doubleValue());
      } else if (value != null && value.getClass().isAssignableFrom(Double.class)) {
        if (!Double.isNaN((Double) value) && Double.isFinite((Double) value)) {
          addDataPoint(name, (Double) value);
        }
      } else if (value != null && value instanceof Number) {
        addDataPoint(name, ((Number) value).doubleValue());
      }
    }

    private void collectCounter(String name, Counter counter) {
      addDataPoint(name, counter.getCount());
    }


    private void collectHistogram(String name, Histogram histogram) {
      TagEncodedMetricName rootMetric = TagEncodedMetricName.decode(name);
      addDataPoint(rootMetric.submetric("count"), histogram.getCount());
      reportSnapshot(rootMetric, histogram.getSnapshot());
    }

    private void collectMeter(String name, Meter meter) {
      TagEncodedMetricName rootMetric = TagEncodedMetricName.decode(name);
      addDataPoint(rootMetric.submetric("count"), meter.getCount());
      printMetered(rootMetric, meter);
    }

    private void collectTimer(String name, Timer timer) {
      TagEncodedMetricName rootMetric = TagEncodedMetricName.decode(name);
      addDataPoint(rootMetric.submetric("count"), timer.getCount());
      printMetered(rootMetric, timer);
      reportSnapshot(rootMetric, timer.getSnapshot());
    }


    private void reportSnapshot(TagEncodedMetricName metric, Snapshot snapshot) {
      addDataPoint(metric.submetric("duration.min"), convertDuration(snapshot.getMin()));
      addDataPoint(metric.submetric("duration.max"), convertDuration(snapshot.getMax()));
      addDataPoint(metric.submetric("duration.mean"), convertDuration(snapshot.getMean()));
      addDataPoint(metric.submetric("duration.stddev"), convertDuration(snapshot.getStdDev()));
      addDataPoint(metric.submetric("duration", "quantile", "p50"),
          convertDuration(snapshot.getMedian()));
      addDataPoint(metric.submetric("duration", "quantile", "p75"),
          convertDuration(snapshot.get75thPercentile()));
      addDataPoint(metric.submetric("duration", "quantile", "p95"),
          convertDuration(snapshot.get95thPercentile()));
      addDataPoint(metric.submetric("duration", "quantile", "p98"),
          convertDuration(snapshot.get98thPercentile()));
      addDataPoint(metric.submetric("duration", "quantile", "p99"),
          convertDuration(snapshot.get99thPercentile()));
      addDataPoint(metric.submetric("duration", "quantile", "p999"),
          convertDuration(snapshot.get999thPercentile()));
    }

    private void printMetered(TagEncodedMetricName metric, Metered meter) {
      addDataPoint(metric.submetric("rate", "window", "1m"),
          convertRate(meter.getOneMinuteRate()));
      addDataPoint(metric.submetric("rate", "window", "5m"),
          convertRate(meter.getFiveMinuteRate()));
      addDataPoint(metric.submetric("rate", "window", "15m"),
          convertRate(meter.getFifteenMinuteRate()));
      //addDataPoint(rootMetric.submetric("rate", "window", "all"), epoch, meter.getMeanRate());
    }

    private double convertRate(double rate) {
      return ApptuitReporter.this.convertRate(rate);
    }

    private double convertDuration(double duration) {
      return ApptuitReporter.this.convertDuration(duration);
    }

    private void addDataPoint(String name, double value) {
      addDataPoint(TagEncodedMetricName.decode(name), value);
    }

    private void addDataPoint(String name, long value) {
      addDataPoint(TagEncodedMetricName.decode(name), value);
    }

    private void addDataPoint(TagEncodedMetricName name, Number value) {

      /*
      //TODO support disabled metric attributes
      if(getDisabledMetricAttributes().contains(type)) {
          return;
      }
      */

      DataPoint dataPoint = new DataPoint(name.getMetricName(), epoch, value, name.tags);
      dataPoints.add(dataPoint);
      debug(dataPoint);
    }
  }

  public interface DataPointsReporter {

    void put(Collection<DataPoint> dataPoints);
  }
}
