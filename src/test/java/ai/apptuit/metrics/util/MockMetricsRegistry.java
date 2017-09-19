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
import org.powermock.api.mockito.PowerMockito;

/**
 * @author <a href="mailto:rajiv.shivane@apptuit.ai">Rajiv</a>
 * @since 9/15/2017
 */
public class MockMetricsRegistry {

  private static final MockMetricsRegistry instance = new MockMetricsRegistry();

  static {
    try {
      initialize();
    } catch (Exception e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  private MockMetricsRegistry() {
  }

  public static MockMetricsRegistry getInstance() {
    return instance;
  }

  public static void initialize() throws Exception {
    System.out.println("<<Mocking MetricRegist"
        + "ry>>");
    MetricRegistry mockRegistry = mock(MetricRegistry.class);
    PowerMockito.whenNew(MetricRegistry.class).withNoArguments().thenReturn(mockRegistry);

    when(mockRegistry.timer(anyString())).then(invocationOnMock -> {
      Object metricName = invocationOnMock.getArguments()[0];
      System.out.printf("#MetricRegistry.getTimer(\"%s\")\n", metricName);
      Timer mock = mock(Timer.class);
      when(mock.time()).then(invocationOnMock1 -> {
        System.out.printf("#MetricRegistry.startTimer(\"%s\")\n", metricName);
        Context ctxt = mock(Context.class);
        when(ctxt.stop()).then(invocationOnMock2 -> {
          System.out.printf("#MetricRegistry.stopTimer(\"%s\")\n", metricName);
          return 0;
        });
        return ctxt;
      });
      return mock;
    });
  }
}
