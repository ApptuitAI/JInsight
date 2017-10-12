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

import ai.apptuit.metrics.jinsight.modules.common.RuleHelper;
import ai.apptuit.metrics.jinsight.modules.logback.LogEventTracker;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.jboss.byteman.rule.Rule;

/**
 * @author Rajiv Shivane
 */
public class Log4J2RuleHelper extends RuleHelper {

  private static final LogEventTracker tracker = new LogEventTracker();

  public Log4J2RuleHelper(Rule rule) {
    super(rule);
  }

  public void appendersCalled(LogEvent event) {
    ThrowableProxy throwableProxy = event.getThrownProxy();
    String throwable = (throwableProxy != null) ? throwableProxy.getName() : null;
    LogEventTracker.LogLevel level = LogEventTracker.LogLevel.valueOf(event.getLevel().toString());
    tracker.track(level, (throwableProxy != null), throwable);

  }

}
