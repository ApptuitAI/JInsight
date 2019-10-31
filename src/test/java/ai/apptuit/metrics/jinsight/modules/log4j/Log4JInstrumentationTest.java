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

import static ai.apptuit.metrics.jinsight.modules.logback.LogEventTracker.APPENDS_BASE_NAME;
import static ai.apptuit.metrics.jinsight.modules.logback.LogEventTracker.FINGERPRINTS_BASE_NAME;
import static ai.apptuit.metrics.jinsight.modules.logback.LogEventTracker.THROWABLES_BASE_NAME;
import static org.junit.Assert.assertEquals;

import ai.apptuit.metrics.client.TagEncodedMetricName;
import ai.apptuit.metrics.jinsight.RegistryService;
import ai.apptuit.metrics.jinsight.modules.logback.ErrorFingerprint;
import ai.apptuit.metrics.jinsight.modules.logback.LogEventTracker;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.WriterAppender;
import org.apache.log4j.spi.LoggingEvent;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


/**
 * @author Rajiv Shivane
 */
public class Log4JInstrumentationTest {

  private MetricRegistry registry;
  private Map<String, Meter> meters;
  private TestAppender testAppender;
  private Logger logger;
  private Level origLevel;
  private Throwable testException;
  private ErrorFingerprint expectedFingerprint;

  @Before
  public void setUp() throws Exception {
    registry = RegistryService.getMetricRegistry();

    testException = new RuntimeException();
    testException.setStackTrace(new StackTraceElement[0]);
    expectedFingerprint = ErrorFingerprint.fromThrowable(testException);

    meters = new HashMap<>();
    meters.put("total", getMeter(APPENDS_BASE_NAME.submetric("total")));
    meters.put("trace", getMeter(APPENDS_BASE_NAME.withTags("level", "trace")));
    meters.put("debug", getMeter(APPENDS_BASE_NAME.withTags("level", "debug")));
    meters.put("info", getMeter(APPENDS_BASE_NAME.withTags("level", "info")));
    meters.put("warn", getMeter(APPENDS_BASE_NAME.withTags("level", "warn")));
    meters.put("error", getMeter(APPENDS_BASE_NAME.withTags("level", "error")));
    meters.put("fatal", getMeter(APPENDS_BASE_NAME.withTags("level", "fatal")));
    meters.put("throwCount", getMeter(THROWABLES_BASE_NAME.submetric("total")));
    meters.put("throw[RuntimeException]",
        getMeter(THROWABLES_BASE_NAME.withTags("class", testException.getClass().getName())
        ));
    meters.put("fingerprint[RuntimeException]",
        getMeter(FINGERPRINTS_BASE_NAME.withTags("class", testException.getClass().getName())
            .withTags("fingerprint", expectedFingerprint.getChecksum())
        ));

    logger = getLogger(Log4JInstrumentationTest.class.getName());
    origLevel = logger.getLevel();
    logger.setLevel(Level.ALL);

  }

  private Logger getLogger(String name) {
    Properties properties = new Properties();
    properties.setProperty("log4j.rootCategory", "INFO,TestLog");
    properties.setProperty("log4j.appender.TestLog", TestAppender.class.getName());
    properties.setProperty("log4j.appender.TestLog.layout", "org.apache.log4j.PatternLayout");
    properties.setProperty("log4j.appender.TestLog.layout.ConversionPattern", "%d [%t] %p %c %x - %m%n");
    PropertyConfigurator.configure(properties);

    Logger logger = Logger.getLogger(name);
    testAppender = (TestAppender) logger.getParent().getAppender("TestLog");
    return logger;
  }

  @After
  public void tearDown() throws Exception {
    logger.setLevel(origLevel);
  }

  @Test
  public void testThrowable() throws Exception {
    Map<String, Long> expectedCounts = getCurrentCounts();
    expectedCounts.compute("total", (s, aLong) -> aLong + 1);
    expectedCounts.compute("error", (s, aLong) -> aLong + 1);
    expectedCounts.compute("throwCount", (s, aLong) -> aLong + 1);
    expectedCounts.compute("throw[RuntimeException]", (s, aLong) -> aLong + 1);
    expectedCounts.compute("fingerprint[RuntimeException]", (s, aLong) -> aLong + 1);

    logger.error("Error with throwable", testException);

    assertEquals(expectedCounts, getCurrentCounts());
    assertEquals(expectedFingerprint.getChecksum(), testAppender.getFingerprint());
    Assert.assertThat(testAppender.getLogContent(),
        CoreMatchers.containsString("[error:" + expectedFingerprint.getChecksum() + "]"));

  }

  @Test
  public void testLogTrace() throws Exception {
    Map<String, Long> expectedCounts = getCurrentCounts();
    expectedCounts.compute("total", (s, aLong) -> aLong + 1);
    expectedCounts.compute("trace", (s, aLong) -> aLong + 1);

    logger.trace("TRACE!");

    assertEquals(expectedCounts, getCurrentCounts());
  }


  @Test
  public void testLogDebug() throws Exception {
    Map<String, Long> expectedCounts = getCurrentCounts();
    expectedCounts.compute("total", (s, aLong) -> aLong + 1);
    expectedCounts.compute("debug", (s, aLong) -> aLong + 1);

    logger.debug("DEBUG!");

    assertEquals(expectedCounts, getCurrentCounts());
  }


  @Test
  public void testLogInfo() throws Exception {
    Map<String, Long> expectedCounts = getCurrentCounts();
    expectedCounts.compute("total", (s, aLong) -> aLong + 1);
    expectedCounts.compute("info", (s, aLong) -> aLong + 1);

    logger.info("INFO!");

    assertEquals(expectedCounts, getCurrentCounts());
  }


  @Test
  public void testLogWarn() throws Exception {
    Map<String, Long> expectedCounts = getCurrentCounts();
    expectedCounts.compute("total", (s, aLong) -> aLong + 1);
    expectedCounts.compute("warn", (s, aLong) -> aLong + 1);

    logger.warn("WARN!");

    assertEquals(expectedCounts, getCurrentCounts());
  }

  @Test
  public void testLogError() throws Exception {
    Map<String, Long> expectedCounts = getCurrentCounts();
    expectedCounts.compute("total", (s, aLong) -> aLong + 1);
    expectedCounts.compute("error", (s, aLong) -> aLong + 1);

    logger.error("ERROR!");

    assertEquals(expectedCounts, getCurrentCounts());
  }


  @Test
  public void testLogFatal() throws Exception {
    Map<String, Long> expectedCounts = getCurrentCounts();
    expectedCounts.compute("total", (s, aLong) -> aLong + 1);
    expectedCounts.compute("fatal", (s, aLong) -> aLong + 1);

    logger.fatal("FATAL!");

    assertEquals(expectedCounts, getCurrentCounts());
  }

  @Test
  public void testLogLevel() throws Exception {
    logger.setLevel(Level.ERROR);

    Map<String, Long> expectedCounts = getCurrentCounts();
    expectedCounts.compute("total", (s, aLong) -> aLong + 2);
    expectedCounts.compute("error", (s, aLong) -> aLong + 1);
    expectedCounts.compute("fatal", (s, aLong) -> aLong + 1);

    logger.trace("trace!");
    logger.debug("debug!");
    logger.info("info!");
    logger.warn("warn!");
    logger.error("error!");
    logger.fatal("fatal!");

    assertEquals(expectedCounts, getCurrentCounts());
  }

  @Test
  public void testLoggerReconfiguration() throws Exception {
    logger.setLevel(Level.ERROR);

    Map<String, Long> expectedCounts = getCurrentCounts();
    expectedCounts.compute("total", (s, aLong) -> aLong + 1);
    expectedCounts.compute("error", (s, aLong) -> aLong + 1);

    Properties properties = new Properties();
    properties.setProperty("log4j.rootCategory", "INFO,TestLog");
    properties.setProperty("log4j.appender.TestLog", "org.apache.log4j.ConsoleAppender");
    properties.setProperty("log4j.appender.TestLog.layout", "org.apache.log4j.PatternLayout");
    PropertyConfigurator.configure(properties);

    logger.error("error!");
    assertEquals(expectedCounts, getCurrentCounts());

  }

  private Meter getMeter(TagEncodedMetricName name) {
    return registry.meter(name.toString());
  }

  private Map<String, Long> getCurrentCounts() {
    Map<String, Long> currentValues = new HashMap<>(meters.size());
    meters.forEach((k, meter) -> {
      currentValues.put(k, meter.getCount());
    });
    return currentValues;
  }

  public static class TestAppender extends WriterAppender {

    private ByteArrayOutputStream logBuffer = new ByteArrayOutputStream();
    private final OutputStreamWriter writer = new OutputStreamWriter(logBuffer, Charset.defaultCharset());
    private Object fingerprint;

    public TestAppender() {
      super();
      setWriter(writer);
    }

    @Override
    public void append(LoggingEvent event) {
      super.append(event);
      fingerprint = event.getMDC(LogEventTracker.FINGERPRINT_PROPERTY_NAME);
    }

    public Object getFingerprint() {
      return fingerprint;
    }

    public String getLogContent() throws IOException {
      writer.flush();
      return logBuffer.toString();
    }
  }
}
