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
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.jboss.byteman.rule.Rule;

/**
 * @author Rajiv Shivane
 */
public class Log4J2RuleHelper extends RuleHelper {

  public static final TagEncodedMetricName ROOT_NAME = TagEncodedMetricName.decode("log4j.appends");

  public void instrument(org.apache.logging.log4j.core.config.LoggerConfig config){
    config.addAppender(new InstrumentedAppender(), null, null);
  }

  public Log4J2RuleHelper(Rule rule) {
    super(rule);
  }

  private static class InstrumentedAppender extends AbstractAppender {

    private static final MetricRegistry registry = RegistryService.getMetricRegistry();

    private static Meter total = registry.meter(ROOT_NAME.submetric("total").toString());

    private static Meter trace = registry.meter(ROOT_NAME.withTags("level", "trace").toString());
    private static Meter debug = registry.meter(ROOT_NAME.withTags("level", "debug").toString());
    private static Meter info = registry.meter(ROOT_NAME.withTags("level", "info").toString());
    private static Meter warn = registry.meter(ROOT_NAME.withTags("level", "warn").toString());
    private static Meter error = registry.meter(ROOT_NAME.withTags("level", "error").toString());
    private static Meter fatal = registry.meter(ROOT_NAME.withTags("level", "fatal").toString());

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

    }
  }
}
