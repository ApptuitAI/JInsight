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

import ai.apptuit.metrics.client.TagEncodedMetricName;
import ai.apptuit.metrics.jinsight.RegistryService;
import ai.apptuit.metrics.jinsight.modules.common.RuleHelper;
import com.codahale.metrics.Clock;
import com.codahale.metrics.Timer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.jboss.byteman.rule.Rule;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Transaction;
import redis.clients.util.Pool;

/**
 * @author Rajiv Shivane
 */
public class JedisRuleHelper extends RuleHelper {


  public static final TagEncodedMetricName COMMANDS_BASE_NAME =
      TagEncodedMetricName.decode("jedis.commands");
  public static final TagEncodedMetricName TRANSACTIONS_EXEC_METRIC =
      TagEncodedMetricName.decode("jedis.transactions.exec");
  public static final TagEncodedMetricName TRANSACTIONS_DISCARD_METRIC =
      TagEncodedMetricName.decode("jedis.transactions.discard");
  public static final TagEncodedMetricName PIPELINES_SYNC_METRIC =
      TagEncodedMetricName.decode("jedis.pipelines.sync");
  public static final TagEncodedMetricName POOL_GET_METRIC =
      TagEncodedMetricName.decode("jedis.pool.get");
  public static final TagEncodedMetricName POOL_RELEASE_METRIC =
      TagEncodedMetricName.decode("jedis.pool.release");

  private static final Clock clock = Clock.defaultClock();

  private static final String TRANSACTION_START_TIME_PROP_NAME = "jedis.tx.start";

  private static final Timer txExecTimer = RegistryService.getMetricRegistry()
      .timer(TRANSACTIONS_EXEC_METRIC.toString());
  private static final Timer txDiscardTimer = RegistryService.getMetricRegistry()
      .timer(TRANSACTIONS_DISCARD_METRIC.toString());
  private static final Timer pipelineSyncTimer = RegistryService.getMetricRegistry()
      .timer(PIPELINES_SYNC_METRIC.toString());

  private static final OperationId POOL_GET_OPERATION = new OperationId("jedis.pool.get");
  private static final OperationId POOL_RELEASE_OPERATION = new OperationId("jedis.pool.release");
  private static final Timer poolGetTimer = RegistryService.getMetricRegistry()
      .timer(POOL_GET_METRIC.toString());
  private static final Timer poolReleaseTimer = RegistryService.getMetricRegistry()
      .timer(POOL_RELEASE_METRIC.toString());


  private static final Map<String, OperationId> operations = new ConcurrentHashMap<>();
  private static final Map<String, Timer> timers = new ConcurrentHashMap<>();
  private static final Map<String, String> commands = new ConcurrentHashMap<>();

  static {
    commands.put("append", "append");
    commands.put("bitcount", "bitcount");
    commands.put("bitfield", "bitfield");
    commands.put("bitpos", "bitpos");
    commands.put("blpop", "blpop");
    commands.put("brpop", "brpop");
    commands.put("decr", "decr");
    commands.put("decrBy", "decrBy");
    commands.put("del", "delete");
    commands.put("echo", "echo");
    commands.put("exists", "exists");
    commands.put("expire", "expire");
    commands.put("expireAt", "expireAt");
    commands.put("geoadd", "geoadd");
    commands.put("geodist", "geodist");
    commands.put("geohash", "geohash");
    commands.put("geopos", "geopos");
    commands.put("georadius", "georadius");
    commands.put("georadiusByMember", "georadiusByMember");
    commands.put("get", "get");
    commands.put("getbit", "getbit");
    commands.put("getrange", "getrange");
    commands.put("getSet", "getSet");
    commands.put("hdel", "hdel");
    commands.put("hexists", "hexists");
    commands.put("hget", "hget");
    commands.put("hgetAll", "hgetAll");
    commands.put("hincrBy", "hincrBy");
    commands.put("hincrByFloat", "hincrByFloat");
    commands.put("hkeys", "hkeys");
    commands.put("hlen", "hlen");
    commands.put("hmget", "hmget");
    commands.put("hmset", "hmset");
    commands.put("hscan", "hscan");
    commands.put("hset", "hset");
    commands.put("hsetnx", "hsetnx");
    commands.put("hvals", "hvals");
    commands.put("incr", "incr");
    commands.put("incrBy", "incrBy");
    commands.put("incrByFloat", "incrByFloat");
    commands.put("lindex", "lindex");
    commands.put("linsert", "linsert");
    commands.put("llen", "llen");
    commands.put("lpop", "lpop");
    commands.put("lpush", "lpush");
    commands.put("lpushx", "lpushx");
    commands.put("lrange", "lrange");
    commands.put("lrem", "lrem");
    commands.put("lset", "lset");
    commands.put("ltrim", "ltrim");
    commands.put("move", "move");
    commands.put("persist", "persist");
    commands.put("pexpire", "pexpire");
    commands.put("pexpireAt", "pexpireAt");
    commands.put("pfadd", "pfadd");
    commands.put("pfcount", "pfcount");
    commands.put("psetex", "psetex");
    commands.put("pttl", "pttl");
    commands.put("rpop", "rpop");
    commands.put("rpush", "rpush");
    commands.put("rpushx", "rpushx");
    commands.put("sadd", "sadd");
    commands.put("scard", "scard");
    commands.put("set", "set");
    commands.put("setbit", "setbit");
    commands.put("setex", "setex");
    commands.put("setnx", "setnx");
    commands.put("setrange", "setrange");
    commands.put("sismember", "sismember");
    commands.put("smembers", "smembers");
    commands.put("sort", "sort");
    commands.put("spop", "spop");
    commands.put("srandmember", "srandmember");
    commands.put("srem", "srem");
    commands.put("sscan", "sscan");
    commands.put("strlen", "strlen");
    commands.put("substr", "substr");
    commands.put("ttl", "ttl");
    commands.put("type", "type");
    commands.put("zadd", "zadd");
    commands.put("zcard", "zcard");
    commands.put("zcount", "zcount");
    commands.put("zincrby", "zincrby");
    commands.put("zlexcount", "zlexcount");
    commands.put("zrange", "zrange");
    commands.put("zrangeByLex", "zrangeByLex");
    commands.put("zrangeByScore", "zrangeByScore");
    commands.put("zrangeByScoreWithScores", "zrangeByScoreWithScores");
    commands.put("zrangeWithScores", "zrangeWithScores");
    commands.put("zrank", "zrank");
    commands.put("zrem", "zrem");
    commands.put("zremrangeByLex", "zremrangeByLex");
    commands.put("zremrangeByRank", "zremrangeByRank");
    commands.put("zremrangeByScore", "zremrangeByScore");
    commands.put("zrevrange", "zrevrange");
    commands.put("zrevrangeByLex", "zrevrangeByLex");
    commands.put("zrevrangeByScore", "zrevrangeByScore");
    commands.put("zrevrangeByScoreWithScores", "zrevrangeByScoreWithScores");
    commands.put("zrevrangeWithScores", "zrevrangeWithScores");
    commands.put("zrevrank", "zrevrank");
    commands.put("zscan", "zscan");
    commands.put("zscore", "zscore");
  }


  public JedisRuleHelper(Rule rule) {
    super(rule);
  }

  public void onOperationStart(String methName, Jedis jedis) {
    OperationId operationId = getOperationId(methName);
    if (operationId == null) {
      return;//not tracking metrics for this method
    }
    beginTimedOperation(operationId);
  }

  public void onOperationEnd(String methName, Jedis jedis) {
    OperationId operationId = getOperationId(methName);
    if (operationId == null) {
      return;//not tracking metrics for this method
    }
    endTimedOperation(operationId, getTimer(methName));
  }

  public void onOperationError(String methName, Jedis jedis) {
    OperationId operationId = getOperationId(methName);
    if (operationId == null) {
      return;//not tracking metrics for this method
    }
    endTimedOperation(operationId, getTimer(methName));
  }

  public void onTransactionBegin(Transaction tx) {
    setObjectProperty(tx, TRANSACTION_START_TIME_PROP_NAME, clock.getTick());
  }

  public void onTransactionExec(Transaction tx) {
    Long startTime = removeObjectProperty(tx, TRANSACTION_START_TIME_PROP_NAME);
    if (startTime == null) {
      return;
    }

    long t = Clock.defaultClock().getTick() - startTime;
    txExecTimer.update(t, TimeUnit.NANOSECONDS);
  }

  public void onTransactionDiscard(Transaction tx) {
    Long startTime = removeObjectProperty(tx, TRANSACTION_START_TIME_PROP_NAME);
    if (startTime == null) {
      return;
    }
    long t = Clock.defaultClock().getTick() - startTime;
    txDiscardTimer.update(t, TimeUnit.NANOSECONDS);
  }

  public void onPipelineBegin(Pipeline pipeline) {
    setObjectProperty(pipeline, TRANSACTION_START_TIME_PROP_NAME, clock.getTick());
  }

  public void onPipelineSync(Pipeline pipeline) {
    Long startTime = removeObjectProperty(pipeline, TRANSACTION_START_TIME_PROP_NAME);
    if (startTime == null) {
      return;
    }

    long t = Clock.defaultClock().getTick() - startTime;
    pipelineSyncTimer.update(t, TimeUnit.NANOSECONDS);
  }

  public void onPoolGetStart(Pool pool) {
    beginTimedOperation(POOL_GET_OPERATION);
  }

  public void onPoolGetEnd(Pool pool) {
    endTimedOperation(POOL_GET_OPERATION, poolGetTimer);
  }

  public void onPoolGetError(Pool pool) {
    endTimedOperation(POOL_GET_OPERATION, poolGetTimer);
  }


  public void onPoolReleaseStart(Pool pool) {
    beginTimedOperation(POOL_RELEASE_OPERATION);
  }

  public void onPoolReleaseEnd(Pool pool) {
    endTimedOperation(POOL_RELEASE_OPERATION, poolReleaseTimer);
  }

  public void onPoolReleaseError(Pool pool) {
    endTimedOperation(POOL_RELEASE_OPERATION, poolReleaseTimer);
  }


  private OperationId getOperationId(String methName) {
    return operations.computeIfAbsent(methName, s -> new OperationId("jedis." + methName));
  }

  private Timer getTimer(String methName) {
    String op = commands.get(methName);
    return timers.computeIfAbsent(op, s -> {
      String metric = COMMANDS_BASE_NAME.withTags("command", op).toString();
      return RegistryService.getMetricRegistry().timer(metric);
    });
  }

}
