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
import java.lang.reflect.Method;
import org.jboss.byteman.rule.Rule;

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
      MdcProxy.setMDC(LogEventTracker.FINGERPRINT_PROPERTY_NAME, fingerprint.getChecksum());
    }
  }

  public void afterBuildEvent(Throwable throwable) {
    if (throwable == null) {
      return;
    }
    CURRENT_FINGERPRINT.remove();
    MdcProxy.removeMDC(LogEventTracker.FINGERPRINT_PROPERTY_NAME);
  }

  public String convertMessage(ILoggingEvent event, String origMessage) {
    String fingerprint = event.getMDCPropertyMap().get(LogEventTracker.FINGERPRINT_PROPERTY_NAME);
    if (fingerprint == null) {
      return origMessage;
    }
    return "[error:" + fingerprint + "] " + origMessage;
  }

  /**
   * Wrapper to access slf4f.MDC
   * We use reflection to access MDC as the slf4j package is being relocated and we want these properties to be set
   * on the MDC loaded by the application and not on the relocated MDC packaged with JInsight
   */
  static class MdcProxy {

    private static final String CLASS_NAME = new StringBuilder().append("org.").append("slf4j.").append("MDC")
        .toString();

    public static void setMDC(String propertyName, String propertyValue) {
      try {
        Class<?> mdcClass = Class.forName(CLASS_NAME);
        Method putMethod = mdcClass.getMethod("put", String.class, String.class);
        putMethod.invoke(null, propertyName, propertyValue);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    public static void removeMDC(String propertyName) {
      try {
        Class<?> mdcClass = Class.forName(CLASS_NAME);
        Method removeMethod = mdcClass.getMethod("remove", String.class);
        removeMethod.invoke(null, propertyName);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}
