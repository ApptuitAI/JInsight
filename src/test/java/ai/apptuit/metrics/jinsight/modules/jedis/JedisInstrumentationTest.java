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

package ai.apptuit.metrics.jinsight.modules.jedis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import ai.apptuit.metrics.jinsight.RegistryService;
import ai.apptuit.metrics.jinsight.testing.CountTracker;
import ai.apptuit.metrics.jinsight.testing.CountTracker.Snapshot;
import com.codahale.metrics.MetricRegistry;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.Jedis;


/**
 * @author Rajiv Shivane
 */
public class JedisInstrumentationTest {

  private Map<String, Object> presetElements;
  private List<String> presetElementKeys;

  private Jedis jedis;
  private CountTracker tracker;


  @Before
  public void setUp() throws Exception {
    MetricRegistry registry = RegistryService.getMetricRegistry();

    presetElements = IntStream.range(0, 1000).boxed()
        .collect(Collectors.toMap(i -> UUID.randomUUID().toString(), i -> i));
    presetElementKeys = new ArrayList<>(presetElements.keySet());

    String redisAddr = System.getProperty("redis.addr");

    jedis = new Jedis(new URI("redis://" + redisAddr));

    tracker = new CountTracker(registry, JedisRuleHelper.COMMANDS_BASE_NAME, "command");
    tracker.registerTimer("set");
    tracker.registerTimer("get");
    tracker.registerTimer("delete");
    tracker.registerTimer("exists");
    tracker.registerTimer("append");
    tracker.registerTimer("incr");
    tracker.registerTimer("decr");
    tracker.registerTimer("lpush");
    tracker.registerTimer("lpop");
    tracker.registerTimer("sadd");
    tracker.registerTimer("spop");
    tracker.registerTimer("zadd");
    tracker.registerTimer("zrem");
  }

  @After
  public void tearDown() throws Exception {
    jedis.close();
  }

  @Test
  public void testSet() throws Exception {
    int rnd = ThreadLocalRandom.current().nextInt(0, presetElements.size());
    String key = presetElementKeys.get(rnd);

    Snapshot snapshot = tracker.snapshot();
    snapshot.increment("set");
    jedis.set(key, String.valueOf(presetElements.get(key)));

    snapshot.validate();
  }


  @Test
  public void testGet() throws Exception {
    int rnd = ThreadLocalRandom.current().nextInt(0, presetElements.size());
    String key = presetElementKeys.get(rnd);
    jedis.set(key, String.valueOf(presetElements.get(key)));

    Snapshot snapshot = tracker.snapshot();
    snapshot.increment("get");
    String val = jedis.get(key);
    assertEquals(String.valueOf(presetElements.get(key)), val);

    snapshot.validate();
  }

  @Test
  public void testDelete() throws Exception {
    int rnd = ThreadLocalRandom.current().nextInt(0, presetElements.size());
    String key = presetElementKeys.get(rnd);
    jedis.set(key, String.valueOf(presetElements.get(key)));

    Snapshot snapshot = tracker.snapshot();
    snapshot.increment("delete");
    Long del = jedis.del(key);
    assertEquals(1L, (long) del);

    snapshot.validate();
    assertNull(jedis.get(key));
  }

  @Test
  public void testAppend() throws Exception {
    int rnd = ThreadLocalRandom.current().nextInt(0, presetElements.size());
    String key = presetElementKeys.get(rnd);
    String value = String.valueOf(presetElements.get(key));
    jedis.set(key, value);

    Snapshot snapshot = tracker.snapshot();
    snapshot.increment("append");
    String value1 = UUID.randomUUID().toString();
    long appended = jedis.append(key, value1);
    assertEquals((value + value1).length(), appended);
    snapshot.validate();

    Object val = jedis.get(key);
    assertEquals(value + value1, val);
  }

  @Test
  public void testIncr() throws Exception {
    int rnd = ThreadLocalRandom.current().nextInt(0, presetElements.size());
    String key = presetElementKeys.get(rnd);
    String value = String.valueOf(presetElements.get(key));
    jedis.set(key, value);

    Snapshot snapshot = tracker.snapshot();
    snapshot.increment("incr");
    long incremented = jedis.incr(key);
    assertEquals(Integer.valueOf(value) + 1, incremented);
    snapshot.validate();
  }

  @Test
  public void testDecr() throws Exception {
    int rnd = ThreadLocalRandom.current().nextInt(0, presetElements.size());
    String key = presetElementKeys.get(rnd);
    String value = String.valueOf(presetElements.get(key));
    jedis.set(key, value);

    Snapshot snapshot = tracker.snapshot();
    snapshot.increment("decr");
    long decremented = jedis.decr(key);
    assertEquals(Integer.valueOf(value) - 1, decremented);
    snapshot.validate();
  }

  @Test
  public void testListPushPop() throws Exception {
    int rnd = ThreadLocalRandom.current().nextInt(0, presetElements.size());
    String key = presetElementKeys.get(rnd);
    String value = String.valueOf(presetElements.get(key));
    jedis.lpush(key, value);

    Snapshot snapshot = tracker.snapshot();
    snapshot.increment("lpush");
    String value1 = UUID.randomUUID().toString();
    long len = jedis.lpush(key, value1);
    assertEquals(2, len);
    snapshot.validate();

    snapshot.increment("lpop");
    String val2 = jedis.lpop(key);
    assertEquals(value1, val2);
    snapshot.validate();
  }

  @Test
  public void testSetAddPop() throws Exception {
    int rnd = ThreadLocalRandom.current().nextInt(0, presetElements.size());
    String key = presetElementKeys.get(rnd);
    String value = String.valueOf(presetElements.get(key));

    Snapshot snapshot = tracker.snapshot();
    snapshot.increment("sadd");
    Long added = jedis.sadd(key, value);
    assertEquals(1, (long) added);

    snapshot.increment("spop");
    String val2 = jedis.spop(key);
    assertEquals(value, val2);
    snapshot.validate();
  }

  @Test
  public void testSortedSetAddRemove() throws Exception {
    int rnd = ThreadLocalRandom.current().nextInt(0, presetElements.size());
    String key = presetElementKeys.get(rnd);
    String value = String.valueOf(presetElements.get(key));

    Snapshot snapshot = tracker.snapshot();
    snapshot.increment("zadd");
    Long added = jedis.zadd(key, 1, value);
    assertEquals(1, (long) added);

    snapshot.increment("zrem");
    Long removed = jedis.zrem(key, value);
    assertEquals(1, (long) removed);
    snapshot.validate();
  }
}
