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

import ai.apptuit.metrics.dropwizard.TagEncodedMetricName;
import ai.apptuit.metrics.jinsight.RegistryService;
import ai.apptuit.metrics.jinsight.modules.common.RuleHelper;
import com.codahale.metrics.Clock;
import com.codahale.metrics.Timer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import net.spy.memcached.ops.ConcatenationOperation;
import net.spy.memcached.ops.GetAndTouchOperation;
import net.spy.memcached.ops.GetOperation;
import net.spy.memcached.ops.GetsOperation;
import net.spy.memcached.ops.MutatorOperation;
import net.spy.memcached.ops.Operation;
import net.spy.memcached.ops.StoreOperation;
import org.jboss.byteman.rule.Rule;

/**
 * @author Rajiv Shivane
 */
public class SpymemcachedRuleHelper extends RuleHelper {

  public static final TagEncodedMetricName ROOT_NAME = TagEncodedMetricName
      .decode("memcached.commands");

  private static final String OPERATION_PROPERTY_NAME = "spymemcached.Operation";
  private static final Clock clock = Clock.defaultClock();
  private static final Map<String, Timer> opVsTimer = new ConcurrentHashMap<>();

  public SpymemcachedRuleHelper(Rule rule) {
    super(rule);
  }

  public void onOperationCreate(Operation operation) {
    Long startTime = getObjectProperty(operation, OPERATION_PROPERTY_NAME);
    if (startTime != null) {
      return;//re-entrant
    }

    setObjectProperty(operation, OPERATION_PROPERTY_NAME, clock.getTick());
  }

  public void onCallbackComplete(Operation operation) {
    Long startTime = removeObjectProperty(operation, OPERATION_PROPERTY_NAME);
    if (startTime == null) {
      return;//re-entrant
    }

    String op = Operations.getOperationName(operation);

    long t = clock.getTick() - startTime;
    Timer timer = opVsTimer.computeIfAbsent(op, s -> {
      String metricName = ROOT_NAME.withTags("command", op).toString();
      return RegistryService.getMetricRegistry().timer(metricName);
    });
    timer.update(t, TimeUnit.NANOSECONDS);

  }

  static class Operations {

    private static final Map<String, String> classToOperationMap = new ConcurrentHashMap<>();

    public static <T extends Operation> String getOperationName(T operation) {

      //TODO optimize. Convert multiple if/else to switch or map.get()
      String op;
      if (operation instanceof StoreOperation) {
        op = ((StoreOperation) operation).getStoreType().name();
      } else if (operation instanceof ConcatenationOperation) {
        op = ((ConcatenationOperation) operation).getStoreType().name();
      } else if (operation instanceof GetsOperation) {
        op = "get";
      } else if (operation instanceof GetOperation) {
        op = "get";
      } else if (operation instanceof GetAndTouchOperation) {
        op = "gat";
      } else if (operation instanceof MutatorOperation) {
        op = ((MutatorOperation) operation).getType().name();
      } else {
        op = Operations.getOperationName(operation.getClass());
      }
      return op;
    }

    public static String getOperationName(Class<? extends Operation> clazz) {
      String simpleName = clazz.getSimpleName();
      String opName = classToOperationMap.get(simpleName);
      if (opName != null) {
        return opName;
      }

      opName = simpleName;
      opName = opName.replaceAll("OperationImpl$", "");
      opName = opName.replaceAll("([A-Z])([A-Z]*)([A-Z])", "$1$2_$3");
      opName = opName.replaceAll("([^A-Z_])([A-Z])", "$1_$2").toLowerCase();
      classToOperationMap.put(simpleName, opName);
      return opName;
    }
  }

}
