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

package ai.apptuit.metrics.jinsight.modules.spymemcached;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import ai.apptuit.metrics.jinsight.RegistryService;
import ai.apptuit.metrics.jinsight.testing.CountTracker;
import ai.apptuit.metrics.jinsight.testing.CountTracker.Snapshot;
import com.codahale.metrics.MetricRegistry;
import com.github.zafarkhaja.semver.Version;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.spy.memcached.AddrUtil;
import net.spy.memcached.CASResponse;
import net.spy.memcached.CASValue;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.internal.OperationFuture;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Rajiv Shivane
 */
public abstract class AbstractSpyMemcachedInstrumentationTest {


  protected MemcachedClient memcacheClient;
  protected CountTracker tracker;

  @Before
  public void setUp() throws Exception {

    System.setProperty("net.spy.log.LoggerImpl", "net.spy.memcached.compat.log.SunLogger");
    Logger.getLogger("net.spy.memcached").setLevel(Level.WARNING);

    String memcachedAddr = System.getProperty("memcached.addr");
    List<InetSocketAddress> addresses = AddrUtil.getAddresses(memcachedAddr);
    memcacheClient = getMemcacheClient(addresses);
    InetSocketAddress address = addresses.get(0);
    Map<String, String> stats = memcacheClient.getStats().get(address);
    assertTrue(
        "Couldnt get stats for memcache server. Verify memcached is running at " + address,
        stats.size() > 0);
    //Fail the setup if we can not connect to the default server

    MetricRegistry metricRegistry = RegistryService.getMetricRegistry();

    tracker = new CountTracker(metricRegistry, SpymemcachedRuleHelper.ROOT_NAME, "command") {
      @Override
      public void validate(Snapshot snapshot) {
        try {
          Thread.sleep(200); //wait for metrics to be updated in the registry
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        super.validate(snapshot);
      }
    };
    tracker.registerTimer("get");
    tracker.registerTimer("set");
    tracker.registerTimer("append");
    tracker.registerTimer("prepend");
    tracker.registerTimer("add");
    tracker.registerTimer("replace");
    tracker.registerTimer("touch");
    tracker.registerTimer("gat");
    tracker.registerTimer("incr");
    tracker.registerTimer("decr");
    tracker.registerTimer("delete");
  }

  @Test
  public void testSetAndGet() throws Exception {
    String value = UUID.randomUUID().toString();
    Snapshot expectedValues = tracker.snapshot();

    expectedValues.increment("set");
    Boolean didSet = memcacheClient.set("key", 0, value).get();
    assertTrue(didSet);
    tracker.validate(expectedValues);

    expectedValues.increment("get");
    Object val = memcacheClient.get("key");
    assertEquals(value, val);
    tracker.validate(expectedValues);
  }


  @Test
  public void testAppend() throws Exception {
    String value = UUID.randomUUID().toString();
    Boolean didSet = memcacheClient.set("key", 0, value).get();
    assertTrue(didSet);

    Snapshot expectedValues = tracker.snapshot();
    expectedValues.increment("append");
    String value1 = UUID.randomUUID().toString();
    Boolean appended = memcacheClient.append("key", value1).get();
    assertTrue(appended);
    tracker.validate(expectedValues);

    Object val = memcacheClient.get("key");
    assertEquals(value + value1, val);
  }


  @Test
  public void testPrepend() throws Exception {
    String value = UUID.randomUUID().toString();
    Boolean didSet = memcacheClient.set("key", 0, value).get();
    assertTrue(didSet);

    Snapshot expectedValues = tracker.snapshot();
    expectedValues.increment("prepend");
    String value1 = UUID.randomUUID().toString();
    Boolean appended = memcacheClient.prepend("key", value1).get();
    assertTrue(appended);
    tracker.validate(expectedValues);

    Object val = memcacheClient.get("key");
    assertEquals(value1 + value, val);
  }


  @Test
  public void testAdd() throws Exception {
    String value = UUID.randomUUID().toString();
    Boolean didSet = memcacheClient.set("key", 0, value).get();
    assertTrue(didSet);

    String value1 = UUID.randomUUID().toString();
    Boolean added = memcacheClient.add("key", 0, value1).get();
    assertFalse(added);

    Snapshot expectedValues = tracker.snapshot();
    expectedValues.increment("add");
    String key1 = UUID.randomUUID().toString();
    added = memcacheClient.add(key1, 0, value1).get();
    assertTrue(added);
    tracker.validate(expectedValues);

    Object val = memcacheClient.get(key1);
    assertEquals(value1, val);
  }


  @Test
  public void testReplace() throws Exception {
    String value = UUID.randomUUID().toString();
    Boolean didSet = memcacheClient.set("key", 0, value).get();
    assertTrue(didSet);

    String key1 = UUID.randomUUID().toString();
    String value1 = UUID.randomUUID().toString();
    Boolean added = memcacheClient.replace(key1, 0, value1).get();
    assertFalse(added);

    Snapshot expectedValues = tracker.snapshot();
    expectedValues.increment("replace");
    added = memcacheClient.replace("key", 0, value1).get();
    assertTrue(added);
    tracker.validate(expectedValues);

    Object val = memcacheClient.get("key");
    assertEquals(value1, val);
  }


  @Test
  public void testAsyncGet() throws Exception {
    String value = UUID.randomUUID().toString();
    Boolean didSet = memcacheClient.set("key", 0, value).get();
    assertTrue(didSet);

    Snapshot expectedValues = tracker.snapshot();
    expectedValues.increment("get");
    Object val = memcacheClient.asyncGet("key").get();
    assertEquals(value, val);
    tracker.validate(expectedValues);
  }


  @Test
  public void testAsyncGets() throws Exception {
    String value = UUID.randomUUID().toString();
    Boolean didSet = memcacheClient.set("key", 0, value).get();
    assertTrue(didSet);

    Snapshot expectedValues = tracker.snapshot();
    expectedValues.increment("get");
    CASValue<Object> casValue = memcacheClient.asyncGets("key").get();
    assertEquals(value, casValue.getValue());
    tracker.validate(expectedValues);

    String value1 = UUID.randomUUID().toString();
    CASResponse response = memcacheClient.cas("key", casValue.getCas(), value1);
    assertEquals(CASResponse.OK, response);
  }

  @Test
  public void testGets() throws Exception {
    String value = UUID.randomUUID().toString();
    Boolean didSet = memcacheClient.set("key", 0, value).get();
    assertTrue(didSet);

    Snapshot expectedValues = tracker.snapshot();
    expectedValues.increment("get");
    CASValue<Object> casValue = memcacheClient.gets("key");
    assertEquals(value, casValue.getValue());
    tracker.validate(expectedValues);

    String value1 = UUID.randomUUID().toString();
    CASResponse response = memcacheClient.cas("key", casValue.getCas(), value1);
    assertEquals(CASResponse.OK, response);
  }


  @Test
  public void testAsyncGetBulk() throws Exception {
    String value1 = UUID.randomUUID().toString();
    Boolean didSet = memcacheClient.set("key1", 0, value1).get();
    assertTrue(didSet);
    String value2 = UUID.randomUUID().toString();
    didSet = memcacheClient.set("key2", 0, value2).get();
    assertTrue(didSet);
    String value3 = UUID.randomUUID().toString();
    didSet = memcacheClient.set("key3", 0, value3).get();
    assertTrue(didSet);

    Snapshot expectedValues = tracker.snapshot();
    expectedValues.increment("get");
    Map<String, Object> values = memcacheClient.asyncGetBulk("key1", "key2", "key3").get();
    assertEquals(value1, values.get("key1"));
    assertEquals(value2, values.get("key2"));
    assertEquals(value3, values.get("key3"));
    tracker.validate(expectedValues);
  }


  @Test
  public void testGetBulk() throws Exception {
    String value1 = UUID.randomUUID().toString();
    Boolean didSet = memcacheClient.set("key1", 0, value1).get();
    assertTrue(didSet);
    String value2 = UUID.randomUUID().toString();
    didSet = memcacheClient.set("key2", 0, value2).get();
    assertTrue(didSet);
    String value3 = UUID.randomUUID().toString();
    didSet = memcacheClient.set("key3", 0, value3).get();
    assertTrue(didSet);

    Snapshot expectedValues = tracker.snapshot();
    expectedValues.increment("get");
    Map<String, Object> values = memcacheClient.getBulk("key1", "key2", "key3");
    assertEquals(value1, values.get("key1"));
    assertEquals(value2, values.get("key2"));
    assertEquals(value3, values.get("key3"));
    tracker.validate(expectedValues);
  }


  @Test
  public void testTouch() throws Exception {
    if (serverSupportsTouch()) {
      return;
    }
    String value = UUID.randomUUID().toString();
    Boolean didSet = memcacheClient.set("key", 0, value).get();
    assertTrue(didSet);

    Snapshot expectedValues = tracker.snapshot();
    expectedValues.increment("touch");
    OperationFuture<Boolean> operationFuture = memcacheClient.touch("key", 0);
    Boolean touched = operationFuture.get();
    assertTrue(touched);
    tracker.validate(expectedValues);
    /*
    //Touch operation does not return cas value

    String value1 = UUID.randomUUID().toString();
    CASResponse response = memcacheClient.cas("key", operationFuture.getCas(), value1);
    assertEquals(CASResponse.OK, response);
    */
  }

  @Test
  public void testIncrLong() throws Exception {
    long value = 90L;

    //Yes, we need to set it as string!! https://stackoverflow.com/a/19704095
    Boolean didSet = memcacheClient.set("key", 0, String.valueOf(value)).get();
    assertTrue(didSet);

    Snapshot expectedValues = tracker.snapshot();
    expectedValues.increment("incr");
    Long val = memcacheClient.incr("key", 7);
    assertEquals(value + 7, (long) val);
    tracker.validate(expectedValues);
  }


  @Test
  public void testIncrInt() throws Exception {
    int value = 90;

    //Yes, we need to set it as string!! https://stackoverflow.com/a/19704095
    Boolean didSet = memcacheClient.set("key", 0, String.valueOf(value)).get();
    assertTrue(didSet);

    Snapshot expectedValues = tracker.snapshot();
    expectedValues.increment("incr");
    long val = memcacheClient.incr("key", 7);
    assertEquals(value + 7, (long) val);
    tracker.validate(expectedValues);
  }


  @Test
  public void testAsyncIncrLong() throws Exception {
    long value = 90L;

    //Yes, we need to set it as string!! https://stackoverflow.com/a/19704095
    Boolean didSet = memcacheClient.set("key", 0, String.valueOf(value)).get();
    assertTrue(didSet);

    Snapshot expectedValues = tracker.snapshot();
    expectedValues.increment("incr");
    Long val = memcacheClient.asyncIncr("key", 7).get();
    assertEquals(value + 7, (long) val);
    tracker.validate(expectedValues);
  }


  @Test
  public void testAsyncIncrInt() throws Exception {
    int value = 90;

    //Yes, we need to set it as string!! https://stackoverflow.com/a/19704095
    Boolean didSet = memcacheClient.set("key", 0, String.valueOf(value)).get();
    assertTrue(didSet);

    Snapshot expectedValues = tracker.snapshot();
    expectedValues.increment("incr");
    long val = memcacheClient.asyncIncr("key", 7).get();
    assertEquals(value + 7, (long) val);
    tracker.validate(expectedValues);
  }


  @Test
  public void testDecrLong() throws Exception {
    long value = 90L;

    //Yes, we need to set it as string!! https://stackoverflow.com/a/19704095
    Boolean didSet = memcacheClient.set("key", 0, String.valueOf(value)).get();
    assertTrue(didSet);

    Snapshot expectedValues = tracker.snapshot();
    expectedValues.increment("decr");
    Long val = memcacheClient.decr("key", 7);
    assertEquals(value - 7, (long) val);
    tracker.validate(expectedValues);
  }


  @Test
  public void testDecrInt() throws Exception {
    int value = 90;

    //Yes, we need to set it as string!! https://stackoverflow.com/a/19704095
    Boolean didSet = memcacheClient.set("key", 0, String.valueOf(value)).get();
    assertTrue(didSet);

    Snapshot expectedValues = tracker.snapshot();
    expectedValues.increment("decr");
    long val = memcacheClient.decr("key", 7);
    assertEquals(value - 7, (long) val);
    tracker.validate(expectedValues);
  }


  @Test
  public void testAsyncDecrLong() throws Exception {
    long value = 90L;

    //Yes, we need to set it as string!! https://stackoverflow.com/a/19704095
    Boolean didSet = memcacheClient.set("key", 0, String.valueOf(value)).get();
    assertTrue(didSet);

    Snapshot expectedValues = tracker.snapshot();
    expectedValues.increment("decr");
    Long val = memcacheClient.asyncDecr("key", 7).get();
    assertEquals(value - 7, (long) val);
    tracker.validate(expectedValues);
  }


  @Test
  public void testAsyncDecrInt() throws Exception {
    int value = 90;

    //Yes, we need to set it as string!! https://stackoverflow.com/a/19704095
    Boolean didSet = memcacheClient.set("key", 0, String.valueOf(value)).get();
    assertTrue(didSet);

    Snapshot expectedValues = tracker.snapshot();
    expectedValues.increment("decr");
    long val = memcacheClient.asyncDecr("key", 7).get();
    assertEquals(value - 7, (long) val);
    tracker.validate(expectedValues);
  }


  @Test
  public void testDelete() throws Exception {
    String value = UUID.randomUUID().toString();
    Boolean didSet = memcacheClient.set("key", 0, value).get();
    assertTrue(didSet);

    Snapshot expectedValues = tracker.snapshot();
    expectedValues.increment("delete");
    Boolean deleted = memcacheClient.delete("key").get();
    assertTrue(deleted);
    tracker.validate(expectedValues);

    Object val = memcacheClient.get("key");
    assertNull(val);
  }

  protected boolean serverSupportsTouch() {
    Map<SocketAddress, String> versions = memcacheClient.getVersions();
    for (String version : versions.values()) {
      System.out.println("memcached version = " + version);
      version = version.replaceAll("([0-9A-Za-z\\-\\.]*).*", "$1");
      if (Version.valueOf(version).lessThan(Version.valueOf("1.4.8"))) {
        System.err.println("Memcache version [" + version + "] less than 1.4.8."
            + " Skipping touch command tests");
        return true;
      }
    }
    return false;
  }

  protected abstract MemcachedClient getMemcacheClient(List<InetSocketAddress> addresses)
      throws IOException;
}
