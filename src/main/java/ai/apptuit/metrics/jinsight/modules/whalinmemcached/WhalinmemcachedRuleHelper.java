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

import ai.apptuit.metrics.client.TagEncodedMetricName;
import ai.apptuit.metrics.jinsight.RegistryService;
import ai.apptuit.metrics.jinsight.modules.common.RuleHelper;
import com.codahale.metrics.Timer;
import com.whalin.MemCached.MemCachedClient;
import org.jboss.byteman.rule.Rule;

/**
 * @author Rajiv Shivane
 */
public class WhalinmemcachedRuleHelper extends RuleHelper {

  public static final TagEncodedMetricName ROOT_NAME = TagEncodedMetricName
      .decode("memcached.commands");

  private static final OperationId SET_OPERATION_ID = new OperationId("whalinmemcached.set");
  private static final OperationId GET_OPERATION_ID = new OperationId("whalinmemcached.get");
  private static final OperationId DELETE_OPERATION_ID = new OperationId("whalinmemcached.delete");

  private static final Timer SET_OPERATION_TIMER = createTimerForOp("set");
  private static final Timer GET_OPERATION_TIMER = createTimerForOp("get");
  private static final Timer DELETE_OPERATION_TIMER = createTimerForOp("delete");
  private static final Timer ADD_OPERATION_TIMER = createTimerForOp("add");
  private static final Timer REPLACE_OPERATION_TIMER = createTimerForOp("replace");
  private static final Timer APPEND_OPERATION_TIMER = createTimerForOp("append");
  private static final Timer PREPEND_OPERATION_TIMER = createTimerForOp("prepend");
  private static final Timer INCR_OPERATION_TIMER = createTimerForOp("incr");
  private static final Timer DECR_OPERATION_TIMER = createTimerForOp("decr");

  public WhalinmemcachedRuleHelper(Rule rule) {
    super(rule);
  }

  private static Timer createTimerForOp(String op) {
    String metric = ROOT_NAME.withTags("command", op).toString();
    return RegistryService.getMetricRegistry().timer(metric);
  }

  public void onOperationStart(String op, MemCachedClient client) {
    beginTimedOperation(getOperationId(op));
  }

  public void onOperationEnd(String op, MemCachedClient client) {
    endTimedOperation(getOperationId(op), getTimerForOp(op));
  }

  public void onOperationError(String op, MemCachedClient client) {
    endTimedOperation(getOperationId(op), getTimerForOp(op));
  }

  private Timer getTimerForOp(String op) {
    switch (op) {
      case "set":
      case "cas":
        return SET_OPERATION_TIMER;
      case "get":
      case "gets":
        return GET_OPERATION_TIMER;
      case "delete":
        return DELETE_OPERATION_TIMER;
      case "add":
        return ADD_OPERATION_TIMER;
      case "replace":
        return REPLACE_OPERATION_TIMER;
      case "append":
        return APPEND_OPERATION_TIMER;
      case "prepend":
        return PREPEND_OPERATION_TIMER;
      case "addOrIncr":
      case "incr":
        return INCR_OPERATION_TIMER;
      case "addOrDecr":
      case "decr":
        return DECR_OPERATION_TIMER;
      default:
        throw new IllegalArgumentException("Unsupported op: " + op);
    }
  }

  private OperationId getOperationId(String op) {
    switch (op) {
      case "set":
      case "cas":
      case "add":
      case "replace":
      case "append":
      case "prepend":
        return SET_OPERATION_ID;
      case "get":
      case "gets":
        return GET_OPERATION_ID;
      case "delete":
        return DELETE_OPERATION_ID;
      case "incr":
      case "decr":
      case "addOrIncr":
      case "addOrDecr":
        return SET_OPERATION_ID;
      default:
        throw new IllegalArgumentException("Unsupported op: " + op);
    }
  }
}
