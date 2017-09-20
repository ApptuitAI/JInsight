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

package ai.apptuit.metrics.dropwizard;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;

import ai.apptuit.metrics.util.MockApptuitPutClient;
import ai.apptuit.metrics.util.MockApptuitPutClient.PutListener;
import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * @author Rajiv Shivane
 */
@PrepareForTest({ApptuitReporter.class})
@PowerMockIgnore({"org.jboss.byteman.*", "javax.net.ssl.*"})
@RunWith(PowerMockRunner.class)
public class ApptuitReporterTest {

  @Test
  public void testReporter() throws Exception {

    MockApptuitPutClient putClient = MockApptuitPutClient.getInstance();

    MetricRegistry registry = new MetricRegistry();

    ApptuitReporterFactory factory = new ApptuitReporterFactory();
    factory.setRateUnit(TimeUnit.SECONDS);
    factory.setDurationUnit(TimeUnit.MILLISECONDS);
    factory.addGlobalTag("globalTag1", "globalValue1");
    factory.setApiKey("dummy");

    ScheduledReporter reporter = factory.build(registry);
    int period = 5;
    reporter.start(period, TimeUnit.SECONDS);

    UUID uuid = UUID.randomUUID();
    String metricName = "ApptuitReporterTest.testReporter." + uuid.toString();
    int expectedCount=2;

    AtomicBoolean foundMetric = new AtomicBoolean(false);
    AtomicInteger lastSeenCount = new AtomicInteger(-1);
    PutListener listener = dataPoints -> {
      dataPoints.forEach(dataPoint -> {
        if (!metricName.equals(dataPoint.getMetric()))
          return;
        int i = dataPoint.getValue().intValue();
        lastSeenCount.set(i);
        if (i != 2)
          return;
        foundMetric.set(true);
      });
    };
    putClient.addPutListener(listener);

    Counter counter = registry.counter(metricName);
    counter.inc();
    counter.inc();


    await().atMost(period*3, TimeUnit.SECONDS).untilTrue(foundMetric);
    putClient.removePutListener(listener);

    assertEquals(expectedCount, lastSeenCount.intValue());
  }
}
