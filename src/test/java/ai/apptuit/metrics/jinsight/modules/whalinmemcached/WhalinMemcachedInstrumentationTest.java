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

package ai.apptuit.metrics.jinsight.modules.whalinmemcached;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import ai.apptuit.metrics.jinsight.RegistryService;
import ai.apptuit.metrics.jinsight.testing.CountTracker;
import ai.apptuit.metrics.jinsight.testing.CountTracker.Snapshot;
import com.codahale.metrics.MetricRegistry;
import com.schooner.MemCached.MemcachedItem;
import com.whalin.MemCached.MemCachedClient;
import com.whalin.MemCached.SockIOPool;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Rajiv Shivane
 */
public class WhalinMemcachedInstrumentationTest {

  private static final String POOL_NAME = "ASCII_POOL";
  protected CountTracker tracker;
  protected MemCachedClient memcacheClient;

  @Before
  public void setUp() throws Exception {
    memcacheClient = getMemcachedClient();

    MetricRegistry metricRegistry = RegistryService.getMetricRegistry();
    tracker = new CountTracker(metricRegistry, WhalinmemcachedRuleHelper.ROOT_NAME, "command");
    tracker.registerTimer("get");
    tracker.registerTimer("set");
    tracker.registerTimer("delete");
    tracker.registerTimer("add");
    tracker.registerTimer("replace");
    tracker.registerTimer("append");
    tracker.registerTimer("prepend");
    tracker.registerTimer("incr");
    tracker.registerTimer("decr");
  }

  @Test
  public void testSetAndGet() throws Exception {
    String value = UUID.randomUUID().toString();

    Snapshot expectedValues = tracker.snapshot();
    expectedValues.increment("set");
    boolean set = memcacheClient.set("key", value);
    assertTrue(set);
    tracker.validate(expectedValues);

    expectedValues.increment("get");
    Object val = memcacheClient.get("key");
    assertEquals(value, val);
    tracker.validate(expectedValues);
  }


  @Test
  public void testDelete() throws Exception {
    String value = UUID.randomUUID().toString();
    boolean set = memcacheClient.set("key", value);
    assertTrue(set);

    Snapshot expectedValues = tracker.snapshot();
    expectedValues.increment("delete");
    boolean deleted = memcacheClient.delete("key");
    assertTrue(deleted);
    tracker.validate(expectedValues);

    Object val = memcacheClient.get("key");
    assertNull(val);
  }


  @Test
  public void testAdd() throws Exception {
    String value = UUID.randomUUID().toString();
    boolean set = memcacheClient.set("key", value);
    assertTrue(set);

    String value1 = UUID.randomUUID().toString();
    boolean added = memcacheClient.add("key", value1);
    assertFalse(added);

    Snapshot expectedValues = tracker.snapshot();
    expectedValues.increment("add");
    String key1 = UUID.randomUUID().toString();
    added = memcacheClient.add(key1, value1);
    assertTrue(added);
    tracker.validate(expectedValues);

    Object val = memcacheClient.get(key1);
    assertEquals(value1, val);
  }


  @Test
  public void testReplace() throws Exception {
    String value = UUID.randomUUID().toString();
    boolean set = memcacheClient.set("key", value);
    assertTrue(set);

    String key1 = UUID.randomUUID().toString();
    String value1 = UUID.randomUUID().toString();
    boolean replaced = memcacheClient.replace(key1, value1);
    assertFalse(replaced);

    Snapshot expectedValues = tracker.snapshot();
    expectedValues.increment("replace");
    replaced = memcacheClient.replace("key", value1);
    assertTrue(replaced);
    tracker.validate(expectedValues);

    Object val = memcacheClient.get("key");
    assertEquals(value1, val);
  }


  @Test
  public void testAppend() throws Exception {
    String value = UUID.randomUUID().toString();
    boolean didSet = memcacheClient.set("key", value);
    assertTrue(didSet);

    Snapshot expectedValues = tracker.snapshot();
    expectedValues.increment("append");
    String value1 = UUID.randomUUID().toString();
    boolean appended = memcacheClient.append("key", value1);
    assertTrue(appended);
    tracker.validate(expectedValues);

    Object val = memcacheClient.get("key");
    assertEquals(value + value1, val);
  }

  @Test
  public void testPrepend() throws Exception {
    String value = UUID.randomUUID().toString();
    boolean didSet = memcacheClient.set("key", value);
    assertTrue(didSet);

    Snapshot expectedValues = tracker.snapshot();
    expectedValues.increment("prepend");
    String value1 = UUID.randomUUID().toString();
    boolean appended = memcacheClient.prepend("key", value1);
    assertTrue(appended);
    tracker.validate(expectedValues);

    Object val = memcacheClient.get("key");
    assertEquals(value1 + value, val);
  }


  @Test
  public void testGetsAndCas() throws Exception {

    String value = UUID.randomUUID().toString();
    boolean didSet = memcacheClient.set("key", value);
    assertTrue(didSet);

    Snapshot expectedValues = tracker.snapshot();
    expectedValues.increment("get");
    MemcachedItem memcachedItem = memcacheClient.gets("key");
    assertEquals(value, memcachedItem.getValue());
    long cas = memcachedItem.casUnique;

    expectedValues.increment("set");
    String value1 = UUID.randomUUID().toString();
    boolean updated = memcacheClient.cas("key", value1, cas);
    assertTrue(updated);
    tracker.validate(expectedValues);

    expectedValues.increment("set");
    updated = memcacheClient.cas("key", value1, (cas << 2) + 1);
    assertFalse(updated);
    tracker.validate(expectedValues);
  }


  @Test
  public void testIncr() throws Exception {
    int value = 90;

    //Yes, we need to set it as string!! https://stackoverflow.com/a/19704095
    boolean didSet = memcacheClient.set("key", String.valueOf(value));
    assertTrue(didSet);

    Snapshot expectedValues = tracker.snapshot();
    expectedValues.increment("incr");
    long val = memcacheClient.incr("key", 7);
    assertEquals(value + 7, (long) val);
    tracker.validate(expectedValues);
  }

  @Test
  public void testAddIncr() throws Exception {
    int value = 90;

    //Yes, we need to set it as string!! https://stackoverflow.com/a/19704095
    boolean didSet = memcacheClient.set("key", String.valueOf(value));
    assertTrue(didSet);

    Snapshot expectedValues = tracker.snapshot();
    expectedValues.increment("incr");
    long val = memcacheClient.addOrIncr("key", 7);
    assertEquals(value + 7, (long) val);
    tracker.validate(expectedValues);
  }


  @Test
  public void testDecr() throws Exception {
    int value = 90;

    //Yes, we need to set it as string!! https://stackoverflow.com/a/19704095
    boolean didSet = memcacheClient.set("key", String.valueOf(value));
    assertTrue(didSet);

    Snapshot expectedValues = tracker.snapshot();
    expectedValues.increment("decr");
    long val = memcacheClient.decr("key", 7);
    assertEquals(value - 7, (long) val);
    tracker.validate(expectedValues);
  }


  @Test
  public void testAddOrDecr() throws Exception {
    int value = 90;

    //Yes, we need to set it as string!! https://stackoverflow.com/a/19704095
    boolean didSet = memcacheClient.set("key", String.valueOf(value));
    assertTrue(didSet);

    Snapshot expectedValues = tracker.snapshot();
    expectedValues.increment("decr");
    long val = memcacheClient.addOrDecr("key", 7);
    assertEquals(value - 7, (long) val);
    tracker.validate(expectedValues);
  }

  protected MemCachedClient getMemcachedClient() {
    String memcachedAddr = System.getProperty("memcached.addr");
    String[] servers = {memcachedAddr};
    SockIOPool pool = SockIOPool.getInstance(POOL_NAME);
    pool.setServers(servers);
    /*
    pool.setFailover( true );
    pool.setInitConn( 10 );
    pool.setMinConn( 5 );
    pool.setMaxConn( 250 );
    pool.setMaintSleep( 30 );
    pool.setNagle( false );
    pool.setSocketTO( 3000 );
    pool.setAliveCheck( true );
    */
    pool.initialize();
    return new MemCachedClient(POOL_NAME, true, false);
  }
}
