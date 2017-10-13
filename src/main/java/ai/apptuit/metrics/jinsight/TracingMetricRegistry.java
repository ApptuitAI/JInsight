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
import com.codahale.metrics.ExponentiallyDecayingReservoir;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import java.util.concurrent.TimeUnit;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * @author Rajiv Shivane
 */
class TracingMetricRegistry extends MetricRegistry {

  private static final Logger metricUpdateTracer = createMetricUpdateTracer();

  private static Logger createMetricUpdateTracer() {
    Logger logger = Logger.getLogger(TracingMetricRegistry.class.getName() + ".metricUpdates");
    ConsoleHandler handler = new ConsoleHandler();
    handler.setFormatter(new Formatter() {
      @Override
      public String format(LogRecord record) {
        return ">>" + record.getMessage() + "\n";
      }
    });
    logger.addHandler(handler);
    logger.setUseParentHandlers(false);
    handler.setLevel(Level.ALL);
    //logger.setLevel(Level.ALL);

    return logger;
  }

  @Override
  public Timer timer(String name) {
    return super.timer(name, () -> new TracedTimer(name));
  }

  @Override
  public Timer timer(String name, MetricSupplier<Timer> supplier) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Meter meter(String name) {
    return super.meter(name, () -> new TracedMeter(name));
  }

  @Override
  public Meter meter(String name, MetricSupplier<Meter> supplier) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Counter counter(String name) {
    return super.counter(name, () -> new TracedCounter(name));
  }

  @Override
  public Counter counter(String name, MetricSupplier<Counter> supplier) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Histogram histogram(String name) {
    return super.histogram(name, () -> new TracedHistogram(name));
  }

  @Override
  public Histogram histogram(String name, MetricSupplier<Histogram> supplier) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Gauge gauge(String name, MetricSupplier<Gauge> supplier) {
    throw new UnsupportedOperationException();
  }

  protected void onUpdate(TraceableMetric metric) {
    String metricType = metric.getClass().getSuperclass().getSimpleName();
    metricUpdateTracer.log(Level.FINE, "Updated " + metricType + ":" + metric.getMetricName());
  }

  interface TraceableMetric extends Metric {

    public String getMetricName();

  }

  private class TracedTimer extends Timer implements TraceableMetric {

    private String metricName;

    public TracedTimer(String metricName) {
      this.metricName = metricName;
    }

    @Override
    public void update(long duration, TimeUnit unit) {
      super.update(duration, unit);
      onUpdate(this);
    }

    public String getMetricName() {
      return metricName;
    }
  }

  private class TracedMeter extends Meter implements TraceableMetric {

    private String metricName;

    public TracedMeter(String metricName) {
      this.metricName = metricName;
    }

    @Override
    public void mark() {
      super.mark(1);
      onUpdate(this);
    }

    @Override
    public void mark(long n) {
      super.mark(n);
      onUpdate(this);
    }

    public String getMetricName() {
      return metricName;
    }
  }

  private class TracedCounter extends Counter implements TraceableMetric {

    private String metricName;

    public TracedCounter(String metricName) {
      this.metricName = metricName;
    }

    @Override
    public void inc() {
      super.inc(1);
      onUpdate(this);
    }

    @Override
    public void inc(long n) {
      super.inc(n);
      onUpdate(this);
    }

    @Override
    public void dec() {
      super.dec(1);
      onUpdate(this);
    }

    @Override
    public void dec(long n) {
      super.dec(n);
      onUpdate(this);
    }

    public String getMetricName() {
      return metricName;
    }
  }

  private class TracedHistogram extends Histogram implements TraceableMetric {

    private String metricName;

    public TracedHistogram(String metricName) {
      super(new ExponentiallyDecayingReservoir());
      this.metricName = metricName;
    }


    @Override
    public void update(int value) {
      super.update((long) value);
      onUpdate(this);

    }

    @Override
    public void update(long value) {
      super.update(value);
      onUpdate(this);
    }

    public String getMetricName() {
      return metricName;
    }
  }
}
