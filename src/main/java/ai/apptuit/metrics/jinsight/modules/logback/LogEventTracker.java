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

  private static final MetricRegistry registry = RegistryService.getMetricRegistry();
  private final TagEncodedMetricName rootName;
  private final TagEncodedMetricName throwablesBaseName;
  private final Meter total;
  private final Meter trace;
  private final Meter debug;
  private final Meter info;
  private final Meter warn;
  private final Meter error;
  private final Meter fatal;
  private final Meter totalThrowables;

  public LogEventTracker(TagEncodedMetricName rootName, TagEncodedMetricName throwablesBaseName) {
    this.rootName = rootName;
    this.throwablesBaseName = throwablesBaseName;

    total = registry.meter(rootName.submetric("total").toString());
    trace = registry.meter(rootName.withTags("level", "trace").toString());
    debug = registry.meter(rootName.withTags("level", "debug").toString());
    info = registry.meter(rootName.withTags("level", "info").toString());
    warn = registry.meter(rootName.withTags("level", "warn").toString());
    error = registry.meter(rootName.withTags("level", "error").toString());
    fatal = registry.meter(rootName.withTags("level", "fatal").toString());
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

  enum LogLevel {
    TRACE, DEBUG, INFO, WARN, ERROR, FATAL
  }

}
