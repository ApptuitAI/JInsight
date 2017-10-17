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

import ai.apptuit.metrics.jinsight.RegistryService;
import ai.apptuit.metrics.jinsight.testing.CountTracker;
import ai.apptuit.metrics.jinsight.testing.CountTracker.Snapshot;
import com.codahale.metrics.MetricRegistry;
import java.net.URI;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;


/**
 * @author Rajiv Shivane
 */
public class JedisPoolInstrumentationTest {

  private JedisPool pool;
  private URI uri;
  private CountTracker getTracker;
  private CountTracker releaseTracker;


  @Before
  public void setUp() throws Exception {
    MetricRegistry registry = RegistryService.getMetricRegistry();

    String redisAddr = System.getProperty("redis.addr");
    uri = new URI("redis://" + redisAddr);

    final JedisPoolConfig poolConfig = new JedisPoolConfig();
    poolConfig.setMaxTotal(5);
    pool = new JedisPool(poolConfig, uri);

    getTracker = new CountTracker(registry, JedisRuleHelper.POOL_GET_METRIC);
    getTracker.registerTimer();
    releaseTracker = new CountTracker(registry, JedisRuleHelper.POOL_RELEASE_METRIC);
    releaseTracker.registerTimer();
  }

  @After
  public void tearDown() throws Exception {
    pool.close();
  }

  @Test
  public void testGetFromPool() throws Exception {
    Snapshot getSnapshot = getTracker.snapshot();
    Snapshot relSnapshot = releaseTracker.snapshot();
    getSnapshot.increment();
    Jedis jedis = pool.getResource();

    relSnapshot.validate();
    getSnapshot.validate();
  }

  @Test
  public void testJedisClose() throws Exception {
    Jedis jedis = pool.getResource();

    Snapshot getSnapshot = getTracker.snapshot();
    Snapshot relSnapshot = releaseTracker.snapshot();
    relSnapshot.increment();
    jedis.close();
    relSnapshot.validate();
    getSnapshot.validate();
  }

  @Test
  public void testJedisNonPooledClose() throws Exception {
    Jedis jedis = new Jedis(uri);

    Snapshot getSnapshot = getTracker.snapshot();
    Snapshot relSnapshot = releaseTracker.snapshot();
    jedis.close();
    relSnapshot.validate();
    getSnapshot.validate();
  }

  @Test
  public void testReturnBrokenResource() throws Exception {
    Jedis jedis = pool.getResource();

    Snapshot getSnapshot = getTracker.snapshot();
    Snapshot relSnapshot = releaseTracker.snapshot();
    relSnapshot.increment();
    pool.returnBrokenResource(jedis);
    relSnapshot.validate();
    getSnapshot.validate();
  }


  @Test
  public void testReturnResource() throws Exception {
    Jedis jedis = pool.getResource();

    Snapshot getSnapshot = getTracker.snapshot();
    Snapshot relSnapshot = releaseTracker.snapshot();
    relSnapshot.increment();
    pool.returnResource(jedis);
    relSnapshot.validate();
    getSnapshot.validate();
  }

  @Test
  public void testReturnResourceObject() throws Exception {
    Jedis jedis = pool.getResource();

    Snapshot getSnapshot = getTracker.snapshot();
    Snapshot relSnapshot = releaseTracker.snapshot();
    relSnapshot.increment();
    pool.returnResourceObject(jedis);
    relSnapshot.validate();
    getSnapshot.validate();
  }

}
