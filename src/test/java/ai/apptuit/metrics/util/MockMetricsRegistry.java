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

package ai.apptuit.metrics.util;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import java.util.HashMap;
import java.util.Map;
import org.powermock.api.mockito.PowerMockito;

/**
 * @author Rajiv Shivane
 */
public class MockMetricsRegistry {
  private static final MockMetricsRegistry instance = new MockMetricsRegistry();

  private Map<String, Integer> metricNameVsNumberOfStartCalls = new HashMap<>();
  private Map<String, Integer> metricNameVsNumberOfStopCalls = new HashMap<>();



  static {
    try {
      initialize();
    } catch (Exception e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  public static void initialize() throws Exception {
    MetricRegistry mockRegistry = mock(MetricRegistry.class);
    PowerMockito.whenNew(MetricRegistry.class).withNoArguments().thenReturn(mockRegistry);

    when(mockRegistry.timer(anyString())).then(invocationOnMock -> {
      String metricName = (String) invocationOnMock.getArguments()[0];
      Timer mock = mock(Timer.class);
      when(mock.time()).then(invocationOnMock1 -> {
        instance.metricNameVsNumberOfStartCalls.put(metricName, instance.getStartCount(metricName)+1);
        Context ctxt = mock(Context.class);
        when(ctxt.stop()).then(invocationOnMock2 -> {
          instance.metricNameVsNumberOfStopCalls.put(metricName, instance.getStopCount(metricName)+1);
          return 0;
        });
        return ctxt;
      });
      return mock;
    });
  }

  private MockMetricsRegistry() {
  }

  public static MockMetricsRegistry getInstance() {
    return instance;
  }

  public int getStartCount(String metricName){
    return metricNameVsNumberOfStartCalls.getOrDefault(metricName, 0);
  }
  public int getStopCount(String metricName){
    return metricNameVsNumberOfStopCalls.getOrDefault(metricName, 0);
  }



}
