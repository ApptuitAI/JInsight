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

package ai.apptuit.metrics.jinsight.modules.log4j2;

import ai.apptuit.metrics.jinsight.RegistryService;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.impl.ThrowableProxy;

/**
 * @author Rajiv Shivane
 */
class InstrumentedAppender extends AbstractAppender {

  private static final MetricRegistry registry = RegistryService.getMetricRegistry();

  private static Meter total = registry
      .meter(Log4J2RuleHelper.ROOT_NAME.submetric("total").toString());

  private static Meter trace = registry
      .meter(Log4J2RuleHelper.ROOT_NAME.withTags("level", "trace").toString());
  private static Meter debug = registry
      .meter(Log4J2RuleHelper.ROOT_NAME.withTags("level", "debug").toString());
  private static Meter info = registry
      .meter(Log4J2RuleHelper.ROOT_NAME.withTags("level", "info").toString());
  private static Meter warn = registry
      .meter(Log4J2RuleHelper.ROOT_NAME.withTags("level", "warn").toString());
  private static Meter error = registry
      .meter(Log4J2RuleHelper.ROOT_NAME.withTags("level", "error").toString());
  private static Meter fatal = registry
      .meter(Log4J2RuleHelper.ROOT_NAME.withTags("level", "fatal").toString());
  private static Meter totalThrowables = registry
      .meter(Log4J2RuleHelper.THROWABLES_BASE_NAME.submetric("total").toString());

  public InstrumentedAppender() {
    super(InstrumentedAppender.class.getName(), null, null);
  }

  @Override
  public void append(LogEvent event) {
    total.mark();
    switch (event.getLevel().getStandardLevel()) {
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

    ThrowableProxy throwableInformation = event.getThrownProxy();
    if (throwableInformation != null) {
      totalThrowables.mark();
      Throwable throwable = throwableInformation.getThrowable();
      if (throwable != null) {
        String className = throwable.getClass().getName();
        registry
            .meter(Log4J2RuleHelper.THROWABLES_BASE_NAME.withTags("class", className).toString())
            .mark();
      }
    }
  }
}
