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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * @author Rajiv Shivane
 */

class Util {

  public static DataPoint jsonToDataPoint(String jsonTxt) throws ParseException {
    JSONParser parser = new JSONParser();
    JSONObject jsonObject = (JSONObject) parser.parse(jsonTxt);
    return toDataPoint(jsonObject);
  }

  public static DataPoint[] jsonToDataPoints(String jsonTxt) throws ParseException {
    List<DataPoint> retVal = new ArrayList<>();
    JSONParser parser = new JSONParser();
    JSONArray jsonObjects = (JSONArray) parser.parse(jsonTxt);
    for (Object o : jsonObjects) {
      JSONObject jsonObject = (JSONObject) o;
      DataPoint dp = toDataPoint(jsonObject);
      retVal.add(dp);
    }
    return retVal.toArray(new DataPoint[retVal.size()]);
  }

  private static DataPoint toDataPoint(JSONObject jsonObject) {
    assertEquals(4, jsonObject.size());
    String metricName = (String) jsonObject.get("metric");
    long epoch = (Long) jsonObject.get("timestamp");
    long value = (Long) jsonObject.get("value");
    Map<String, String> tags = new HashMap<>();

    JSONObject tagsObject = (JSONObject) jsonObject.get("tags");
    Set keys = tagsObject.keySet();
    for (Object key : keys) {
      tags.put((String) key, (String) tagsObject.get(key));
    }
    return new DataPoint(metricName, epoch, value, tags);
  }


  public static void enableHttpClientTracing() {
    java.util.logging.Logger.getLogger("org.apache.http.wire")
        .setLevel(java.util.logging.Level.FINEST);
    java.util.logging.Logger.getLogger("org.apache.http.headers")
        .setLevel(java.util.logging.Level.FINEST);

    System
        .setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
    System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
    System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire", "debug");
    System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "debug");
    System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.headers", "debug");
  }

}
