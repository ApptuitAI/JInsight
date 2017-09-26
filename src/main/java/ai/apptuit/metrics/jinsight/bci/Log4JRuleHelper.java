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

package ai.apptuit.metrics.jinsight.bci;

import ai.apptuit.metrics.dropwizard.TagEncodedMetricName;
import ai.apptuit.metrics.jinsight.RegistryService;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.jboss.byteman.rule.Rule;

/**
 * @author Rajiv Shivane
 */
public class Log4JRuleHelper extends RuleHelper {

  public static final TagEncodedMetricName ROOT_NAME = TagEncodedMetricName.decode("log4j.appends");

  public Log4JRuleHelper(Rule rule) {
    super(rule);
  }

  public void instrumentRootLogger(Logger rootLogger) {
    rootLogger.addAppender(new InstrumentedAppender());
  }

  private static class InstrumentedAppender extends AppenderSkeleton {

    private static final MetricRegistry registry = RegistryService.getMetricRegistry();

    private static Meter total = registry.meter(ROOT_NAME.submetric("total").toString());

    private static Meter trace = registry.meter(ROOT_NAME.withTags("level", "trace").toString());
    private static Meter debug = registry.meter(ROOT_NAME.withTags("level", "debug").toString());
    private static Meter info = registry.meter(ROOT_NAME.withTags("level", "info").toString());
    private static Meter warn = registry.meter(ROOT_NAME.withTags("level", "warn").toString());
    private static Meter error = registry.meter(ROOT_NAME.withTags("level", "error").toString());
    private static Meter fatal = registry.meter(ROOT_NAME.withTags("level", "fatal").toString());


    @Override
    protected void append(LoggingEvent event) {
      total.mark();
      switch (event.getLevel().toInt()) {
        case Level.TRACE_INT:
          trace.mark();
          break;
        case Level.DEBUG_INT:
          debug.mark();
          break;
        case Level.INFO_INT:
          info.mark();
          break;
        case Level.WARN_INT:
          warn.mark();
          break;
        case Level.ERROR_INT:
          error.mark();
          break;
        case Level.FATAL_INT:
          fatal.mark();
          break;
        default:
          break;
      }
    }

    @Override
    public void close() {
    }

    @Override
    public boolean requiresLayout() {
      return false;
    }
  }
}
