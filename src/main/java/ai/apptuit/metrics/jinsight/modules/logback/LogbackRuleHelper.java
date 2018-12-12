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

import ai.apptuit.metrics.jinsight.modules.common.RuleHelper;
import ai.apptuit.metrics.jinsight.modules.logback.LogEventTracker.LogLevel;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import org.jboss.byteman.rule.Rule;
import org.slf4j.MDC;

/**
 * @author Rajiv Shivane
 */
public class LogbackRuleHelper extends RuleHelper {

  private static final LogEventTracker tracker = new LogEventTracker();
  private static final ThreadLocal<ErrorFingerprint> CURRENT_FINGERPRINT = new ThreadLocal<>();

  public LogbackRuleHelper(Rule rule) {
    super(rule);
  }

  public void appendersCalled(ILoggingEvent event) {
    IThrowableProxy throwableProxy = event.getThrowableProxy();
    String throwable = (throwableProxy != null) ? throwableProxy.getClassName() : null;
    LogLevel level = LogLevel.valueOf(event.getLevel().toString());
    ErrorFingerprint fingerprint = CURRENT_FINGERPRINT.get();
    tracker.track(level, (throwableProxy != null), throwable, fingerprint);
  }

  public void beforeBuildEvent(Throwable throwable) {
    if (throwable == null) {
      return;
    }
    ErrorFingerprint fingerprint = ErrorFingerprint.fromThrowable(throwable);
    if (fingerprint != null) {
      CURRENT_FINGERPRINT.set(fingerprint);
      MDC.put(LogEventTracker.FINGERPRINT_PROPERTY_NAME, fingerprint.getChecksum());
    }
  }

  public void afterBuildEvent(Throwable throwable) {
    if (throwable == null) {
      return;
    }
    CURRENT_FINGERPRINT.remove();
    MDC.remove(LogEventTracker.FINGERPRINT_PROPERTY_NAME);
  }

  public String convertMessage(ILoggingEvent event, String origMessage) {
    String fingerprint = event.getMDCPropertyMap().get(LogEventTracker.FINGERPRINT_PROPERTY_NAME);
    if (fingerprint == null) {
      return origMessage;
    }
    return "[error:" + fingerprint + "] " + origMessage;
  }

}
