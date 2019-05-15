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

import static java.nio.charset.StandardCharsets.UTF_8;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Rajiv Shivane
 */
public class MetricRegistryCollection {

  private static final Logger LOGGER = Logger.getLogger(MetricRegistryCollection.class.getName());
  private static final MetricRegistryCollection SINGLETON = new MetricRegistryCollection();

  private final AggregatedMetricRegistry aggregatedMetricRegistry = new AggregatedMetricRegistry();


  private MetricRegistryCollection() {
  }

  void initialize(MetricRegistry jinsightRegistry) {
    registerUnchecked(jinsightRegistry);
  }

  public static MetricRegistryCollection getInstance() {
    return SINGLETON;
  }

  public boolean register(Object metricRegistry) {
    if (!RegistryService.getRegistryService().isInitialized()) {
      throw new IllegalStateException("JInsight not initialized - cannot report ");
    }
    return registerUnchecked(metricRegistry);
  }

  private boolean registerUnchecked(Object metricRegistry) {
    return aggregatedMetricRegistry.registerMetricRegistry(metricRegistry);
  }

  public boolean deRegister(Object metricRegistry) {
    return aggregatedMetricRegistry.deRegisterMetricRegistry(metricRegistry);
  }

  MetricRegistry getAggregatedMetricRegistry() {
    return aggregatedMetricRegistry;
  }

  private static class MetricRegistryWrapper {

    public enum MetricType {
      Gauge("Gauge", GaugeWrapper::new),
      Counter("Counter", CounterWrapper::new),
      Histogram("Histogram", HistogramWrapper::new),
      Meter("Meter", MeterWrapper::new),
      Timer("Timer", TimerWrapper::new);

      private String type;
      private Function constructor;

      MetricType(String type, Function o) {
        this.type = type;
        constructor = o;
      }

      public Method getAccessor(Object delegate) throws NoSuchMethodException {
        Class<?> klass = delegate.getClass();
        return klass.getMethod("get" + type + "s");
      }

      @SuppressWarnings("unchecked")
      public <T extends Metric> T getWrappedValue(Object value) {
        return (T) constructor.apply(value);
      }
    }

    private final Map<MetricType, Method> methods = new WeakHashMap<>();
    private final WeakReference reference;

    public MetricRegistryWrapper(Object delegate) {
      if (delegate == null) {
        throw new IllegalArgumentException("Registry cannot be null");
      }
      this.reference = new WeakReference<>(delegate);
    }

    private Object getDelegate() {
      return reference.get();
    }

    @SuppressWarnings("unchecked")
    public <T extends Metric> void addMetrics(TreeMap<String, T> metrics, MetricType type, MetricFilter filter) {
      try {
        Method method = getAccessorMethod(type);
        Map<String, Object> result = (Map<String, Object>) method.invoke(getDelegate());
        for (Map.Entry<String, Object> entry : result.entrySet()) {
          Metric metric = type.getWrappedValue(entry.getValue());
          String metricName = entry.getKey();
          if (filter.matches(metricName, metric)) {
            metrics.put(metricName, (T) metric);
          }
        }
      } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
        LOGGER.log(Level.SEVERE, "Error collecting metrics from [" + getDelegate() + "]", e);
      }
    }

    private <T extends Metric> Method getAccessorMethod(MetricType type) throws NoSuchMethodException {
      Method method = methods.get(type);
      if (method == null) {
        method = type.getAccessor(getDelegate());
        methods.put(type, method);
      }
      return method;
    }


    private static Object invokeOnDelegate(Object delegate, String methodName) {
      try {
        Method method = delegate.getClass().getMethod(methodName);
        method.setAccessible(true);
        return method.invoke(delegate);
      } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
        throw new RuntimeException(e);
      }
    }

    private static Object invokeOnDelegate(Object delegate, String methodName, double arg) {
      try {
        Method method = delegate.getClass().getMethod(methodName, double.class);
        return method.invoke(delegate, arg);
      } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
        throw new RuntimeException(e);
      }
    }

    private static class GaugeWrapper implements Gauge {

      private Object delegate;

      public GaugeWrapper(Object delegate) {
        this.delegate = delegate;
      }

      @Override
      public Object getValue() {
        return invokeOnDelegate(delegate, "getValue");
      }
    }

    private static class CounterWrapper extends Counter {

      private Object delegate;

      public CounterWrapper(Object delegate) {
        this.delegate = delegate;
      }

      @Override
      public void inc() {
        throw new UnsupportedOperationException();
      }

      @Override
      public void inc(long n) {
        throw new UnsupportedOperationException();
      }

      @Override
      public void dec() {
        throw new UnsupportedOperationException();
      }

      @Override
      public void dec(long n) {
        throw new UnsupportedOperationException();
      }

      @Override
      public long getCount() {
        return (long) invokeOnDelegate(delegate, "getCount");
      }
    }

    private static class MeterWrapper extends Meter {

      private Object delegate;

      public MeterWrapper(Object delegate) {
        this.delegate = delegate;
      }

      @Override
      public void mark() {
        throw new UnsupportedOperationException();

      }

      @Override
      public void mark(long n) {
        throw new UnsupportedOperationException();
      }

      @Override
      public long getCount() {
        return (long) invokeOnDelegate(delegate, "getCount");
      }

      @Override
      public double getFifteenMinuteRate() {
        return (double) invokeOnDelegate(delegate, "getFifteenMinuteRate");
      }

      @Override
      public double getFiveMinuteRate() {
        return (double) invokeOnDelegate(delegate, "getFiveMinuteRate");
      }

      @Override
      public double getMeanRate() {
        return (double) invokeOnDelegate(delegate, "getMeanRate");
      }

      @Override
      public double getOneMinuteRate() {
        return (double) invokeOnDelegate(delegate, "getOneMinuteRate");
      }
    }

    private static class TimerWrapper extends Timer {

      private Object delegate;

      public TimerWrapper(Object delegate) {
        this.delegate = delegate;
      }

      @Override
      public void update(long duration, TimeUnit unit) {
        throw new UnsupportedOperationException();

      }

      @Override
      public <T> T time(Callable<T> event) {
        throw new UnsupportedOperationException();
      }

      @Override
      public void time(Runnable event) {
        throw new UnsupportedOperationException();
      }

      @Override
      public Context time() {
        throw new UnsupportedOperationException();
      }

      @Override
      public long getCount() {
        return (long) invokeOnDelegate(delegate, "getCount");
      }

      @Override
      public double getFifteenMinuteRate() {
        return (double) invokeOnDelegate(delegate, "getFifteenMinuteRate");
      }

      @Override
      public double getFiveMinuteRate() {
        return (double) invokeOnDelegate(delegate, "getFiveMinuteRate");
      }

      @Override
      public double getMeanRate() {
        return (double) invokeOnDelegate(delegate, "getMeanRate");
      }

      @Override
      public double getOneMinuteRate() {
        return (double) invokeOnDelegate(delegate, "getOneMinuteRate");
      }

      @Override
      public Snapshot getSnapshot() {
        return new SnapshotWrapper(invokeOnDelegate(delegate, "getSnapshot"));
      }
    }

    private static class HistogramWrapper extends Histogram {

      private Object delegate;

      public HistogramWrapper(Object delegate) {
        super(null);
        this.delegate = delegate;
      }

      @Override
      public void update(int value) {
        throw new UnsupportedOperationException();
      }

      @Override
      public void update(long value) {
        throw new UnsupportedOperationException();
      }

      @Override
      public long getCount() {
        try {
          Method method = delegate.getClass().getMethod("getCount");
          return (long) method.invoke(delegate);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public Snapshot getSnapshot() {
        try {
          Method method = delegate.getClass().getMethod("getSnapshot");
          return new SnapshotWrapper(method.invoke(delegate));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
          throw new RuntimeException(e);
        }
      }
    }

    private static class SnapshotWrapper extends Snapshot {

      private Object delegate;

      public SnapshotWrapper(Object delegate) {
        this.delegate = delegate;
      }

      @Override
      public double getValue(double quantile) {
        return (double) invokeOnDelegate(delegate, "getValue", quantile);
      }

      @Override
      public long[] getValues() {
        return (long[]) invokeOnDelegate(delegate, "getValues");
      }

      @Override
      public int size() {
        return (int) invokeOnDelegate(delegate, "size");
      }

      @Override
      public long getMax() {
        return (long) invokeOnDelegate(delegate, "getMax");
      }

      @Override
      public double getMean() {
        return (double) invokeOnDelegate(delegate, "getMean");
      }

      @Override
      public long getMin() {
        return (long) invokeOnDelegate(delegate, "getMin");
      }

      @Override
      public double getStdDev() {
        return (double) invokeOnDelegate(delegate, "getStdDev");
      }

      @Override
      public void dump(OutputStream output) {
        try (PrintWriter out = new PrintWriter(new OutputStreamWriter(output, UTF_8))) {
          for (long value : getValues()) {
            out.printf("%d%n", value);
          }
        }
      }
    }

  }

  private static class AggregatedMetricRegistry extends MetricRegistry {

    private final Map<Object, MetricRegistryWrapper> registries = new WeakHashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock writeLock = lock.writeLock();
    private final Lock readLock = lock.readLock();

    public boolean registerMetricRegistry(Object metricRegistry) {
      writeLock.lock();
      try {
        if (registries.containsKey(metricRegistry)) {
          return false;
        }
        registries.put(metricRegistry, new MetricRegistryWrapper(metricRegistry));
        return true;
      } finally {
        writeLock.unlock();
      }
    }

    public boolean deRegisterMetricRegistry(Object metricRegistry) {
      writeLock.lock();
      try {
        return registries.remove(metricRegistry) != null;
      } finally {
        writeLock.unlock();
      }
    }

    @Override
    public SortedMap<String, Gauge> getGauges(MetricFilter filter) {
      return getMetrics(MetricRegistryWrapper.MetricType.Gauge, filter);

    }

    @Override
    public SortedMap<String, Counter> getCounters(MetricFilter filter) {
      return getMetrics(MetricRegistryWrapper.MetricType.Counter, filter);
    }

    @Override
    public SortedMap<String, Histogram> getHistograms(MetricFilter filter) {
      return getMetrics(MetricRegistryWrapper.MetricType.Histogram, filter);
    }

    @Override
    public SortedMap<String, Meter> getMeters(MetricFilter filter) {
      return getMetrics(MetricRegistryWrapper.MetricType.Meter, filter);
    }

    @Override
    public SortedMap<String, Timer> getTimers(MetricFilter filter) {
      return getMetrics(MetricRegistryWrapper.MetricType.Timer, filter);
    }

    private <T extends Metric> SortedMap<String, T> getMetrics(MetricRegistryWrapper.MetricType type,
        MetricFilter filter) {

      final TreeMap<String, T> retVal = new TreeMap<>();
      readLock.lock();
      try {
        for (MetricRegistryWrapper wrapper : registries.values()) {
          wrapper.addMetrics(retVal, type, filter);
        }
      } finally {
        readLock.unlock();
      }
      return Collections.unmodifiableSortedMap(retVal);
    }
  }
}
