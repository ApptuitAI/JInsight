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

package ai.apptuit.metrics.jinsight.modules.logback;

import ai.apptuit.metrics.dropwizard.TagEncodedMetricName;
import ai.apptuit.metrics.jinsight.RegistryService;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;

/**
 * @author Rajiv Shivane
 */
public class LogEventTracker {

  public static final TagEncodedMetricName APPENDS_BASE_NAME = TagEncodedMetricName
      .decode("logger.appends");
  public static final TagEncodedMetricName THROWABLES_BASE_NAME = TagEncodedMetricName
      .decode("logger.throwables");

  private static final MetricRegistry registry = RegistryService.getMetricRegistry();

  private final TagEncodedMetricName throwablesBaseName;
  private final Meter total;
  private final Meter trace;
  private final Meter debug;
  private final Meter info;
  private final Meter warn;
  private final Meter error;
  private final Meter fatal;
  private final Meter totalThrowables;

  public LogEventTracker() {
    this(APPENDS_BASE_NAME, THROWABLES_BASE_NAME);
  }

  private LogEventTracker(TagEncodedMetricName appendsBase, TagEncodedMetricName throwablesBase) {
    this.throwablesBaseName = throwablesBase;

    total = registry.meter(appendsBase.submetric("total").toString());
    trace = registry.meter(appendsBase.withTags("level", "trace").toString());
    debug = registry.meter(appendsBase.withTags("level", "debug").toString());
    info = registry.meter(appendsBase.withTags("level", "info").toString());
    warn = registry.meter(appendsBase.withTags("level", "warn").toString());
    error = registry.meter(appendsBase.withTags("level", "error").toString());
    fatal = registry.meter(appendsBase.withTags("level", "fatal").toString());
    totalThrowables = registry.meter(this.throwablesBaseName.submetric("total").toString());
  }

  public void track(LogLevel level, boolean hasThrowableInfo, String throwableClassName) {
    total.mark();
    switch (level) {
      case TRACE:
        trace.mark();
        break;
      case DEBUG:
        debug.mark();
        break;
      case INFO:
        info.mark();
        break;
      case WARN:
        warn.mark();
        break;
      case ERROR:
        error.mark();
        break;
      case FATAL:
        fatal.mark();
        break;
      default:
        break;
    }

    if (hasThrowableInfo) {
      totalThrowables.mark();
    }

    if (throwableClassName != null) {
      registry.meter(throwablesBaseName.withTags("class", throwableClassName).toString()).mark();
    }
  }

  public enum LogLevel {
    TRACE, DEBUG, INFO, WARN, ERROR, FATAL
  }

}
