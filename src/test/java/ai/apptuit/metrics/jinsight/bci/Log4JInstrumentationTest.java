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
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;


/**
 * @author Rajiv Shivane
 */
public class Log4JInstrumentationTest {

  private MetricRegistry registry;
  private Logger logger;
  private TagEncodedMetricName rootMetric;
  private TagEncodedMetricName totalMetric;

  @Before
  public void setUp() throws Exception {
    registry = RegistryService.getMetricRegistry();
    rootMetric = Log4JRuleHelper.ROOT_NAME;
    totalMetric = rootMetric.submetric("total");

    logger = Logger.getLogger(Log4JInstrumentationTest.class.getName());
    logger.setLevel(Level.ALL);
  }

  @Test
  public void testLogTrace() throws Exception {

    TagEncodedMetricName levelMetric = rootMetric.withTags("level", "trace");
    long expectedCount = getMeterCount(levelMetric) + 1;
    long expectedCountTotal = getMeterCount(totalMetric) + 1;

    logger.trace("TRACE!");

    assertEquals(expectedCountTotal, getMeterCount(totalMetric));
    assertEquals(expectedCount, getMeterCount(levelMetric));
  }


  @Test
  public void testLogDebug() throws Exception {

    TagEncodedMetricName levelMetric = rootMetric.withTags("level", "debug");
    long expectedCount = getMeterCount(levelMetric) + 1;
    long expectedCountTotal = getMeterCount(totalMetric) + 1;

    logger.debug("DEBUG!");

    assertEquals(expectedCountTotal, getMeterCount(totalMetric));
    assertEquals(expectedCount, getMeterCount(levelMetric));
  }


  @Test
  public void testLogInfo() throws Exception {

    TagEncodedMetricName levelMetric = rootMetric.withTags("level", "info");
    long expectedCount = getMeterCount(levelMetric) + 1;
    long expectedCountTotal = getMeterCount(totalMetric) + 1;

    logger.info("INFO!");

    assertEquals(expectedCountTotal, getMeterCount(totalMetric));
    assertEquals(expectedCount, getMeterCount(levelMetric));
  }


  @Test
  public void testLogWarn() throws Exception {

    TagEncodedMetricName levelMetric = rootMetric.withTags("level", "warn");
    long expectedCount = getMeterCount(levelMetric) + 1;
    long expectedCountTotal = getMeterCount(totalMetric) + 1;

    logger.warn("WARN!");

    assertEquals(expectedCountTotal, getMeterCount(totalMetric));
    assertEquals(expectedCount, getMeterCount(levelMetric));
  }

  @Test
  public void testLogError() throws Exception {

    TagEncodedMetricName levelMetric = rootMetric.withTags("level", "error");
    long expectedCount = getMeterCount(levelMetric) + 1;
    long expectedCountTotal = getMeterCount(totalMetric) + 1;

    logger.error("ERROR!");

    assertEquals(expectedCountTotal, getMeterCount(totalMetric));
    assertEquals(expectedCount, getMeterCount(levelMetric));
  }


  @Test
  public void testLogFatal() throws Exception {

    TagEncodedMetricName levelMetric = rootMetric.withTags("level", "fatal");
    long expectedCount = getMeterCount(levelMetric) + 1;
    long expectedCountTotal = getMeterCount(totalMetric) + 1;

    logger.fatal("FATAL!");

    assertEquals(expectedCountTotal, getMeterCount(totalMetric));
    assertEquals(expectedCount, getMeterCount(levelMetric));
  }

  private long getMeterCount(TagEncodedMetricName name) {
    Meter meter = registry.getMeters().get(name.toString());
    return meter != null ? meter.getCount() : 0;
  }
}
