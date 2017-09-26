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

package ai.apptuit.metrics.jinsight;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import org.junit.Test;

/**
 * @author Rajiv Shivane
 */

public class ConfigServiceTest {

  @Test
  public void testGetToken() throws Exception {
    Properties p = new Properties();
    String token = UUID.randomUUID().toString();
    p.setProperty("apptuit.access_token", token);
    ConfigService configService = new ConfigService(p);
    assertEquals(token, configService.getApiToken());
  }

  @Test
  public void testGetTagsMissing() throws Exception {
    Properties p = new Properties();
    ConfigService configService = new ConfigService(p);
    Map<String, String> globalTags = configService.getGlobalTags();
    assertEquals(globalTags, Collections.emptyMap());
  }


  @Test
  public void testGetTags() throws Exception {
    Properties p = new Properties();
    String v1 = UUID.randomUUID().toString();
    String v2 = UUID.randomUUID().toString();
    p.setProperty("global_tags", "k1:"+v1+", k2 : "+v2);
    ConfigService configService = new ConfigService(p);
    Map<String, String> globalTags = configService.getGlobalTags();
    assertEquals(v1, globalTags.get("k1"));
    assertEquals(v2, globalTags.get("k2"));
  }

  @Test(expected = ConfigurationException.class)
  public void testMissingTagValue() throws Exception {
    Properties p = new Properties();
    String v1 = UUID.randomUUID().toString();
    String v2 = UUID.randomUUID().toString();
    p.setProperty("global_tags", "k1: , k2 : "+v2);
    new ConfigService(p);
  }

  @Test(expected = ConfigurationException.class)
  public void testMultipleColons() throws Exception {
    Properties p = new Properties();
    String v1 = UUID.randomUUID().toString();
    String v2 = UUID.randomUUID().toString();
    p.setProperty("global_tags", "k1:"+v1+" k2 : "+v2);
    new ConfigService(p);
  }

  @Test
  public void testDefaultInstance() throws Exception {
    ConfigService cs = ConfigService.getInstance();
    assertEquals("INTEGRATION_TEST_TOKEN", cs.getApiToken());
    assertEquals("testValue1", cs.getGlobalTags().get("testTag1"));
    assertEquals("testValue2", cs.getGlobalTags().get("testTag2"));
  }
}
