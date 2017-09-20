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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import ai.apptuit.metrics.dropwizard.TagEncodedMetricName;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Rajiv Shivane
 */

public class DataPointTest {

  private TagEncodedMetricName tagEncodedMetricName;

  @Before
  public void setUp() throws Exception {
    tagEncodedMetricName = TagEncodedMetricName.decode("proc.stat.cpu")
        .submetric(null, "host", "myhost", "type", "idle");
  }

  @Test
  public void testNotEqualsNull() throws Exception {
    long epoch = System.currentTimeMillis();
    long value = 1515;
    DataPoint dp = new DataPoint(tagEncodedMetricName.getMetricName(), epoch, value,
        Collections.emptyMap());
    assertNotEquals(dp, null);
  }

  @Test
  public void testNotEqualsString() throws Exception {
    long epoch = System.currentTimeMillis();
    long value = 1515;
    DataPoint dp = new DataPoint(tagEncodedMetricName.getMetricName(), epoch, value,
        Collections.emptyMap());
    assertNotEquals(dp, "Text");
  }

  @Test
  public void testEqualsSelf() throws Exception {
    long epoch = System.currentTimeMillis();
    long value = 1515;
    DataPoint dp = new DataPoint(tagEncodedMetricName.getMetricName(), epoch, value,
        Collections.emptyMap());
    assertEquals(dp, dp);
  }

  @Test
  public void testEqualsNoTags() throws Exception {
    long epoch = System.currentTimeMillis();
    long value = 1515;
    DataPoint dp1 = new DataPoint(tagEncodedMetricName.getMetricName(), epoch, value,
        Collections.emptyMap());
    DataPoint dp2 = new DataPoint(tagEncodedMetricName.getMetricName(), epoch, value,
        Collections.emptyMap());
    assertEquals(dp1, dp2);
    assertEquals(dp1.hashCode(), dp2.hashCode());
  }


  @Test
  public void testEqualsWithTags() throws Exception {
    long epoch = System.currentTimeMillis();
    long value = 1515;
    DataPoint dp1 = new DataPoint(tagEncodedMetricName.getMetricName(), epoch, value,
        tagEncodedMetricName.getTags());
    DataPoint dp2 = new DataPoint(tagEncodedMetricName.getMetricName(), epoch, value,
        tagEncodedMetricName.getTags());
    assertEquals(dp1, dp2);
    assertEquals(dp1.hashCode(), dp2.hashCode());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullTags() throws Exception {
    new DataPoint(tagEncodedMetricName.getMetricName(), System.currentTimeMillis(), 1515, null);
  }

  @Test
  public void testToStringNoTags() throws Exception {
    long epoch = System.currentTimeMillis();
    long value = 1515;
    DataPoint dataPoint = new DataPoint(tagEncodedMetricName.getMetricName(), epoch, value,
        Collections.emptyMap());

    assertEquals("proc.stat.cpu " + epoch + " " + value, dataPoint.toString());
  }

  @Test
  public void testToStringWithTags() throws Exception {
    long epoch = System.currentTimeMillis();
    long value = 1515;
    DataPoint dataPoint = new DataPoint(tagEncodedMetricName.getMetricName(),
        epoch, value, tagEncodedMetricName.getTags());

    assertEquals("proc.stat.cpu " + epoch + " " + value + " host=myhost type=idle",
        dataPoint.toString());
  }

  @Test
  public void testToTextNoTags() throws Exception {
    long epoch = System.currentTimeMillis();
    long value = 1515;
    DataPoint dataPoint = new DataPoint(tagEncodedMetricName.getMetricName(), epoch, value,
        Collections.emptyMap());

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    dataPoint.toTextLine(new PrintStream(out), null);

    assertEquals("proc.stat.cpu " + epoch + " " + value + "\n", out.toString());
  }

  @Test
  public void testToTextWithTags() throws Exception {
    long epoch = System.currentTimeMillis();
    long value = 1515;
    DataPoint dataPoint = new DataPoint(tagEncodedMetricName.getMetricName(),
        epoch, value, tagEncodedMetricName.getTags());

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    dataPoint.toTextLine(new PrintStream(out), null);

    assertEquals("proc.stat.cpu " + epoch + " " + value + " host=myhost type=idle\n",
        out.toString());
  }

  @Test
  public void testToTextWithGlobalTags() throws Exception {
    long epoch = System.currentTimeMillis();
    long value = 1515;
    DataPoint dataPoint = new DataPoint(tagEncodedMetricName.getMetricName(),
        epoch, value, Collections.emptyMap());

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    dataPoint.toTextLine(new PrintStream(out), tagEncodedMetricName.getTags());

    assertEquals("proc.stat.cpu " + epoch + " " + value + " host=myhost type=idle\n",
        out.toString());
  }

  @Test
  public void testToJsonNoTags() throws Exception {
    long epoch = System.currentTimeMillis();
    long value = 1515;
    DataPoint dataPoint = new DataPoint(tagEncodedMetricName.getMetricName(), epoch, value,
        Collections.emptyMap());

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    dataPoint.toJson(new PrintStream(out), null);
    String jsonTxt = out.toString();

    DataPoint dp = jsonToDataPoint(jsonTxt);
    assertEquals(dataPoint, dp);
  }

  @Test
  public void testToJsonWithTags() throws Exception {
    long epoch = System.currentTimeMillis();
    long value = 1515;
    DataPoint dataPoint = new DataPoint(tagEncodedMetricName.getMetricName(),
        epoch, value, tagEncodedMetricName.getTags());

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    dataPoint.toJson(new PrintStream(out), null);
    String jsonTxt = out.toString();

    DataPoint dp = jsonToDataPoint(jsonTxt);
    assertEquals(dataPoint, dp);
  }

  @Test
  public void testToJsonWithGlobalTags() throws Exception {
    long epoch = System.currentTimeMillis();
    long value = 1515;
    DataPoint dataPoint = new DataPoint(tagEncodedMetricName.getMetricName(),
        epoch, value, Collections.emptyMap());

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    dataPoint.toJson(new PrintStream(out), tagEncodedMetricName.getTags());
    String jsonTxt = out.toString();

    DataPoint dp = jsonToDataPoint(jsonTxt);
    DataPoint expectedDataPoint = new DataPoint(tagEncodedMetricName.getMetricName(),
        epoch, value, tagEncodedMetricName.getTags());
    assertEquals(expectedDataPoint, dp);
  }

  private DataPoint jsonToDataPoint(String jsonTxt) throws ParseException {
    JSONParser parser = new JSONParser();
    JSONObject jsonObject = (JSONObject) parser.parse(jsonTxt);
    assertEquals(4, jsonObject.size());
    String metricName = (String) jsonObject.get("metric");
    long epoch = (Long) jsonObject.get("timestamp");
    long value = (Long) jsonObject.get("value");
    Map<String, String> tags = new HashMap<>();

    JSONObject tagsObject = (JSONObject) jsonObject.get("tags");
    Set keys = tagsObject.keySet();
    for (Object key : keys) {
      tags.put((String)key, (String)tagsObject.get(key));
    }
    return new DataPoint(metricName, epoch, value, tags);
  }
}
