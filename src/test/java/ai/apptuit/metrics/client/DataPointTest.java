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

import ai.apptuit.metrics.dropwizard.TagEncodedMetricName;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
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

  @Test(expected = IllegalArgumentException.class)
  public void testNullTags() throws Exception {
    new DataPoint(tagEncodedMetricName.getMetricName(), System.currentTimeMillis(), 1515, null);
  }

  @Test
  public void testToStringNoTags() throws Exception {
    long epoch = System.currentTimeMillis();
    int value = 1515;
    DataPoint dataPoint = new DataPoint(tagEncodedMetricName.getMetricName(), epoch, value,
        Collections.EMPTY_MAP);

    assertEquals("proc.stat.cpu " + epoch + " " + value, dataPoint.toString());
  }

  @Test
  public void testToStringWithTags() throws Exception {
    long epoch = System.currentTimeMillis();
    int value = 1515;
    DataPoint dataPoint = new DataPoint(tagEncodedMetricName.getMetricName(),
        epoch, value, tagEncodedMetricName.getTags());

    assertEquals("proc.stat.cpu " + epoch + " " + value + " host=myhost type=idle",
        dataPoint.toString());
  }

  @Test
  public void testToTextNoTags() throws Exception {
    long epoch = System.currentTimeMillis();
    int value = 1515;
    DataPoint dataPoint = new DataPoint(tagEncodedMetricName.getMetricName(), epoch, value,
        Collections.EMPTY_MAP);

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    dataPoint.toTextLine(new PrintStream(out), null);

    assertEquals("proc.stat.cpu " + epoch + " " + value+"\n", out.toString());
  }

  @Test
  public void testToTextWithTags() throws Exception {
    long epoch = System.currentTimeMillis();
    int value = 1515;
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
    int value = 1515;
    DataPoint dataPoint = new DataPoint(tagEncodedMetricName.getMetricName(),
        epoch, value, Collections.emptyMap());

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    dataPoint.toTextLine(new PrintStream(out), tagEncodedMetricName.getTags());

    assertEquals("proc.stat.cpu " + epoch + " " + value + " host=myhost type=idle\n",
        out.toString());
  }
}
