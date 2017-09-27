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

import static org.junit.Assert.assertEquals;

import ai.apptuit.metrics.dropwizard.TagEncodedMetricName;
import ai.apptuit.metrics.jinsight.RegistryService;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;


/**
 * @author Rajiv Shivane
 */
public class Log4JInstrumentationTest {

  private MetricRegistry registry;
  private Map<String, Meter> meters;
  private Logger logger;

  @Before
  public void setUp() throws Exception {
    registry = RegistryService.getMetricRegistry();

    meters = new HashMap<>();
    meters.put("total", getMeter(Log4JRuleHelper.ROOT_NAME.submetric("total")));
    meters.put("trace", getMeter(Log4JRuleHelper.ROOT_NAME.withTags("level", "trace")));
    meters.put("debug", getMeter(Log4JRuleHelper.ROOT_NAME.withTags("level", "debug")));
    meters.put("info", getMeter(Log4JRuleHelper.ROOT_NAME.withTags("level", "info")));
    meters.put("warn", getMeter(Log4JRuleHelper.ROOT_NAME.withTags("level", "warn")));
    meters.put("error", getMeter(Log4JRuleHelper.ROOT_NAME.withTags("level", "error")));
    meters.put("fatal", getMeter(Log4JRuleHelper.ROOT_NAME.withTags("level", "fatal")));

    logger = Logger.getLogger(Log4JInstrumentationTest.class.getName());
    logger.setLevel(Level.ALL);
  }

  @Test
  public void testLogTrace() throws Exception {
    Map<String, Long> expectedCounts = getCurrentCounts();
    expectedCounts.compute("total", (s, aLong) -> aLong+1);
    expectedCounts.compute("trace", (s, aLong) -> aLong+1);

    logger.trace("TRACE!");

    assertEquals(expectedCounts, getCurrentCounts());
  }


  @Test
  public void testLogDebug() throws Exception {
    Map<String, Long> expectedCounts = getCurrentCounts();
    expectedCounts.compute("total", (s, aLong) -> aLong+1);
    expectedCounts.compute("debug", (s, aLong) -> aLong+1);

    logger.debug("DEBUG!");

    assertEquals(expectedCounts, getCurrentCounts());
  }


  @Test
  public void testLogInfo() throws Exception {
    Map<String, Long> expectedCounts = getCurrentCounts();
    expectedCounts.compute("total", (s, aLong) -> aLong+1);
    expectedCounts.compute("info", (s, aLong) -> aLong+1);

    logger.info("INFO!");

    assertEquals(expectedCounts, getCurrentCounts());
  }


  @Test
  public void testLogWarn() throws Exception {
    Map<String, Long> expectedCounts = getCurrentCounts();
    expectedCounts.compute("total", (s, aLong) -> aLong+1);
    expectedCounts.compute("warn", (s, aLong) -> aLong+1);

    logger.warn("WARN!");

    assertEquals(expectedCounts, getCurrentCounts());
  }

  @Test
  public void testLogError() throws Exception {
    Map<String, Long> expectedCounts = getCurrentCounts();
    expectedCounts.compute("total", (s, aLong) -> aLong+1);
    expectedCounts.compute("error", (s, aLong) -> aLong+1);

    logger.error("ERROR!");

    assertEquals(expectedCounts, getCurrentCounts());
  }


  @Test
  public void testLogFatal() throws Exception {
    Map<String, Long> expectedCounts = getCurrentCounts();
    expectedCounts.compute("total", (s, aLong) -> aLong+1);
    expectedCounts.compute("fatal", (s, aLong) -> aLong+1);

    logger.fatal("FATAL!");

    assertEquals(expectedCounts, getCurrentCounts());
  }

  private Meter getMeter(TagEncodedMetricName name) {
    return registry.meter(name.toString());
  }

  private Map<String, Long> getCurrentCounts(){
    Map<String, Long> currentValues = new HashMap<>(meters.size());
    meters.forEach((k, meter)->{
      currentValues.put(k, meter.getCount());
    });
    return currentValues;
  }
}
