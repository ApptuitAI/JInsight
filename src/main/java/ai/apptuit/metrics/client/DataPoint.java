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

package ai.apptuit.metrics.client;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author <a href="mailto:rajiv.shivane@apptuit.ai">Rajiv</a>
 * @since 8/24/2017
 */
public class DataPoint {

  private final String metric;
  private final long timestamp;
  private final Number value;
  private final Map<String, String> tags;

  public DataPoint(String metricName, long epoch, Number value, Map<String, String> tags) {
    this.metric = metricName;
    this.timestamp = epoch;
    this.value = value;
    this.tags = tags;
  }


  public String getMetric() {
    return metric;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public Number getValue() {
    return value;
  }

  public Map<String, String> getTags() {
    return tags;
  }

  @Override
  public String toString() {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(metric).append(" ")
        .append(timestamp).append(" ")
        .append(value);
    for (Entry<String, String> tag : tags.entrySet()) {
      stringBuilder.append(" ").append(tag.getKey())
          .append("=").append(tag.getValue());
    }
    return stringBuilder.toString();
  }

  public void toJson(PrintStream ps, Map<String, String> globalTags) {
    ps.append("{");
    {
      ps.append("\n\"metric\":\"").append(getMetric()).append("\",")
          .append("\n\"timestamp\":").append(Long.toString(getTimestamp())).append(",")
          .append("\n\"value\":").append(String.valueOf(getValue()));
      ps.append(",\n\"tags\": {");

      Map<String, String> tagsToMarshall = new LinkedHashMap<>(getTags());
      tagsToMarshall.putAll(globalTags);
      Iterator<Entry<String, String>> iterator = tagsToMarshall.entrySet().iterator();
      while (iterator.hasNext()) {
        Entry<String, String> tag = iterator.next();
        ps.append("\n\"").append(tag.getKey()).append("\":\"")
            .append(tag.getValue()).append("\"");
        if (iterator.hasNext()) {
          ps.append(",");
        }
      }
      ps.append("}\n");
    }
    ps.append("}");
  }

  public void toTextLine(PrintStream ps, Map<String, String> globalTags) {
    ps.append(getMetric()).append(" ")
        .append(Long.toString(getTimestamp())).append(" ")
        .append(String.valueOf(getValue()));

    Map<String, String> tagsToMarshall = new LinkedHashMap<>(getTags());
    tagsToMarshall.putAll(globalTags);
    tagsToMarshall.forEach((key, value) -> ps.append(" ").append(key).append("=").append(value));
    ps.append('\n');
  }
}
