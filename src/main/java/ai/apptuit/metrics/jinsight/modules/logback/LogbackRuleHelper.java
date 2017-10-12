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
import ai.apptuit.metrics.jinsight.modules.common.RuleHelper;
import ai.apptuit.metrics.jinsight.modules.logback.LogEventTracker.LogLevel;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import org.jboss.byteman.rule.Rule;

/**
 * @author Rajiv Shivane
 */
public class LogbackRuleHelper extends RuleHelper {

  public static final TagEncodedMetricName APPENDS_BASE_NAME = TagEncodedMetricName
      .decode("logback.appends");

  public static final TagEncodedMetricName THROWABLES_BASE_NAME = TagEncodedMetricName
      .decode("logback.throwables");

  private static final LogEventTracker tracker = new LogEventTracker(APPENDS_BASE_NAME, THROWABLES_BASE_NAME);

  public LogbackRuleHelper(Rule rule) {
    super(rule);
  }

  public void appendersCalled(ILoggingEvent event) {
    IThrowableProxy throwableProxy = event.getThrowableProxy();
    String throwable = (throwableProxy != null) ? throwableProxy.getClassName() : null;
    LogLevel level = LogLevel.valueOf(event.getLevel().toString());
    tracker.track(level, (throwableProxy != null), throwable);
  }

}
