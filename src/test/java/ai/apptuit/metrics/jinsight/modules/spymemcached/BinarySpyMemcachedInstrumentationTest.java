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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import ai.apptuit.metrics.jinsight.testing.CountTracker.Snapshot;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.UUID;
import net.spy.memcached.BinaryConnectionFactory;
import net.spy.memcached.CASResponse;
import net.spy.memcached.CASValue;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.internal.OperationFuture;
import org.junit.Test;

/**
 * @author Rajiv Shivane
 */
public class BinarySpyMemcachedInstrumentationTest extends AbstractSpyMemcachedInstrumentationTest {


  @Test
  public void testAppendCas() throws Exception {
    String value = UUID.randomUUID().toString();
    OperationFuture<Boolean> setFuture = memcacheClient.set("key", 0, value);
    Boolean didSet = setFuture.get();
    assertTrue(didSet);
    Long cas = setFuture.getCas();

    Snapshot expectedValues = tracker.snapshot();
    expectedValues.increment("append");
    String value1 = UUID.randomUUID().toString();
    Boolean appended = memcacheClient.append(cas, "key", value1).get();
    assertTrue(appended);
    tracker.validate(expectedValues);

    Object val = memcacheClient.get("key");
    assertEquals(value + value1, val);
  }


  @Test
  public void testPrependCas() throws Exception {
    String value = UUID.randomUUID().toString();
    OperationFuture<Boolean> setFuture = memcacheClient.set("key", 0, value);
    Boolean didSet = setFuture.get();
    assertTrue(didSet);
    Long cas = setFuture.getCas();

    Snapshot expectedValues = tracker.snapshot();
    expectedValues.increment("prepend");
    String value1 = UUID.randomUUID().toString();
    Boolean appended = memcacheClient.prepend(cas, "key", value1).get();
    assertTrue(appended);
    tracker.validate(expectedValues);

    Object val = memcacheClient.get("key");
    assertEquals(value1 + value, val);
  }


  @Test
  public void testAsyncCas() throws Exception {
    String value = UUID.randomUUID().toString();
    OperationFuture<Boolean> setFuture = memcacheClient.set("key", 0, value);
    Boolean didSet = setFuture.get();
    assertTrue(didSet);

    Long cas = setFuture.getCas();
    Snapshot expectedValues = tracker.snapshot();
    expectedValues.increment("set");
    String value1 = UUID.randomUUID().toString();
    CASResponse response = memcacheClient.asyncCAS("key", cas, value1).get();
    assertEquals(CASResponse.OK, response);
    tracker.validate(expectedValues);

    Object val = memcacheClient.get("key");
    assertEquals(value1, val);
  }


  @Test
  public void testCas() throws Exception {
    String value = UUID.randomUUID().toString();
    OperationFuture<Boolean> setFuture = memcacheClient.set("key", 0, value);
    Boolean didSet = setFuture.get();
    assertTrue(didSet);

    Long cas = setFuture.getCas();
    Snapshot expectedValues = tracker.snapshot();
    expectedValues.increment("set");
    String value1 = UUID.randomUUID().toString();
    CASResponse response = memcacheClient.cas("key", cas, value1);
    assertEquals(CASResponse.OK, response);
    tracker.validate(expectedValues);

    Object val = memcacheClient.get("key");
    assertEquals(value1, val);
  }

  @Test
  public void testAsyncGetAndTouch() throws Exception {
    if (serverSupportsTouch()) {
      return;
    }

    String value = UUID.randomUUID().toString();
    Boolean didSet = memcacheClient.set("key", 0, value).get();
    assertTrue(didSet);

    Snapshot expectedValues = tracker.snapshot();
    expectedValues.increment("gat");
    CASValue<Object> casValue = memcacheClient.getAndTouch("key", 0);
    assertEquals(value, casValue.getValue());
    tracker.validate(expectedValues);

    String value1 = UUID.randomUUID().toString();
    CASResponse response = memcacheClient.cas("key", casValue.getCas(), value1);
    assertEquals(CASResponse.OK, response);
  }


  @Test
  public void testGetAndTouch() throws Exception {
    if (serverSupportsTouch()) {
      return;
    }

    String value = UUID.randomUUID().toString();
    Boolean didSet = memcacheClient.set("key", 0, value).get();
    assertTrue(didSet);

    Snapshot expectedValues = tracker.snapshot();
    expectedValues.increment("gat");
    CASValue<Object> casValue = memcacheClient.getAndTouch("key", 0);
    assertEquals(value, casValue.getValue());
    tracker.validate(expectedValues);

    String value1 = UUID.randomUUID().toString();
    CASResponse response = memcacheClient.cas("key", casValue.getCas(), value1);
    assertEquals(CASResponse.OK, response);
  }

  @Test
  public void testDeleteCas() throws Exception {
    String value = UUID.randomUUID().toString();
    OperationFuture<Boolean> future = memcacheClient.set("key", 0, value);
    assertTrue(future.get());
    long cas = future.getCas();

    Snapshot expectedValues = tracker.snapshot();
    expectedValues.increment("delete");
    Boolean deleted = memcacheClient.delete("key", cas).get();
    assertTrue(deleted);
    tracker.validate(expectedValues);

    Object val = memcacheClient.get("key");
    assertNull(val);
  }

  protected MemcachedClient getMemcacheClient(List<InetSocketAddress> addresses)
      throws IOException {
    return new MemcachedClient(new BinaryConnectionFactory(), addresses);

  }
}
