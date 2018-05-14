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

package ai.apptuit.metrics.jinsight.modules.jvm;

import ai.apptuit.metrics.dropwizard.TagEncodedMetricName;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * @author Rajiv Shivane
 */
class BufferPoolMetrics implements MetricSet {

  private static final Logger LOGGER = Logger.getLogger(BufferPoolMetrics.class.getName());

  private static final String[] ATTRIBUTES = {"Count", "MemoryUsed", "TotalCapacity"};
  private static final String[] NAMES = {"count", "used", "capacity"};
  private static final String[] POOLS = {"direct", "mapped"};

  private final MBeanServer mbeanServer;

  public BufferPoolMetrics(MBeanServer mbeanServer) {
    this.mbeanServer = mbeanServer;
  }

  @Override
  public Map<String, Metric> getMetrics() {
    final Map<String, Metric> gauges = new HashMap<String, Metric>();
    for (String pool : POOLS) {
      for (int i = 0; i < ATTRIBUTES.length; i++) {
        final String attribute = ATTRIBUTES[i];
        final String name = NAMES[i];
        try {
          final ObjectName on = new ObjectName("java.nio:type=BufferPool,name=" + pool);
          mbeanServer.getMBeanInfo(on);
          gauges.put(
              TagEncodedMetricName.decode(name).withTags("type", pool).toString(),
              new Gauge<Object>() {
                @Override
                public Object getValue() {
                  try {
                    return mbeanServer.getAttribute(on, attribute);
                  } catch (JMException ignored) {
                    return null;
                  }
                }
              });
        } catch (JMException ignored) {
          LOGGER.fine("Unable to load buffer pool MBeans, needs JDK7 or higher");
        }
      }
    }
    return Collections.unmodifiableMap(gauges);
  }
}
