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

package ai.apptuit.metrics.jinsight.modules.log4j;

import ai.apptuit.metrics.jinsight.modules.common.RuleHelper;
import ai.apptuit.metrics.jinsight.modules.logback.ErrorFingerprint;
import ai.apptuit.metrics.jinsight.modules.logback.LogEventTracker;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;
import org.jboss.byteman.rule.Rule;

/**
 * @author Rajiv Shivane
 */
public class Log4JRuleHelper extends RuleHelper {

  private static final LogEventTracker tracker = new LogEventTracker();


  public Log4JRuleHelper(Rule rule) {
    super(rule);
  }

  public void appendersCalled(LoggingEvent event) {
    ThrowableInformation throwableInfo = event.getThrowableInformation();
    String throwableName = null;
    ErrorFingerprint fingerprint = null;
    if (throwableInfo != null) {
      Throwable throwable = throwableInfo.getThrowable();
      throwableName = (throwable != null) ? throwable.getClass().getName() : null;
      fingerprint = ErrorFingerprint.fromThrowable(throwableInfo.getThrowable());
    }
    LogEventTracker.LogLevel level = LogEventTracker.LogLevel.valueOf(event.getLevel().toString());
    tracker.track(level, (throwableInfo != null), throwableName, fingerprint);

  }

}
