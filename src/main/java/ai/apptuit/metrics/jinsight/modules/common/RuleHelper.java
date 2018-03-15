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

package ai.apptuit.metrics.jinsight.modules.common;

import ai.apptuit.metrics.dropwizard.TagEncodedMetricName;
import ai.apptuit.metrics.jinsight.RegistryService;
import com.codahale.metrics.Clock;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Logger;
import org.jboss.byteman.rule.Rule;
import org.jboss.byteman.rule.helper.Helper;

/**
 * @author Rajiv Shivane
 */
public class RuleHelper extends Helper {

  private static final Logger LOGGER = Logger.getLogger(RuleHelper.class.getName());

  private static final Map<Object, Map<String, Object>> objectProperties = Collections
      .synchronizedMap(new WeakHashMap<>());

  public RuleHelper(Rule rule) {
    super(rule);
  }

  public String setObjectProperty(Object o, String propertyName, String propertyValue) {
    return setObjectProperty0(o, propertyName, propertyValue);
  }

  public Number setObjectProperty(Object o, String propertyName, Number propertyValue) {
    return setObjectProperty0(o, propertyName, propertyValue);
  }

  @SuppressWarnings("unchecked")
  private <V> V setObjectProperty0(Object o, String propertyName, V propertyValue) {
    Map<String, Object> props = objectProperties
        .computeIfAbsent(o, k -> Collections.synchronizedMap(new HashMap<>()));
    return (V) props.put(propertyName, propertyValue);
  }

  @SuppressWarnings("unchecked")
  public <V> V getObjectProperty(Object o, String propertyName) {
    Map<String, Object> props = objectProperties.get(o);
    if (props == null) {
      return null;
    }
    return (V) props.get(propertyName);
  }

  @SuppressWarnings("unchecked")
  public <V> V removeObjectProperty(Object o, String propertyName) {
    Map<String, Object> props = objectProperties.get(o);
    if (props == null) {
      return null;
    }
    return (V) props.remove(propertyName);
  }

  public void beginTimedOperation(OperationId operationId) {
    OperationContexts.start(operationId);
  }

  public void endTimedOperation(OperationId operationId, Timer timer) {
    endTimedOperation(operationId, () -> timer);
  }

  public void endTimedOperation(OperationId operationId,
      Supplier<Timer> timerSupplier) {
    OperationContexts.stop(operationId, timerSupplier);
  }

  protected Timer getTimer(TagEncodedMetricName metricName) {
    MetricRegistry registry = RegistryService.getMetricRegistry();
    return registry.timer(metricName.toString());
  }

  public static final class OperationId {

    private String displayName;

    public OperationId(String displayName) {
      this.displayName = displayName;
    }

    @Override
    public String toString() {
      return "OperationId(" + displayName + ")#" + hashCode();
    }
  }

  private static class OperationContexts {

    private static final ThreadLocal<Deque<OperationContext>> CONTEXT_STACK = ThreadLocal
        .withInitial(LinkedList::new);
    private static final Clock clock = Clock.defaultClock();

    private static final OperationContext RENTRANT = new OperationContext(new OperationId("NOOP"),
        clock);

    public static void start(OperationId id) {
      Deque<OperationContext> contexts = CONTEXT_STACK.get();
      if (!contexts.isEmpty() && lastId() == id) {
        contexts.push(RENTRANT);
        return;
      }
      contexts.push(new OperationContext(id, clock));
    }

    public static void stop(OperationId id, Supplier<Timer> timerSupplier) {
      OperationId lastId = lastId();
      if (lastId != id) {
        //TODO better error handling
        LOGGER.severe("Operation Context Mismatch. Expected: " + id + " got " + lastId);
        return;
      }

      OperationContext context = CONTEXT_STACK.get().pop();
      if (context == RENTRANT) {
        return;
      }

      Timer timer = timerSupplier.get();
      if (timer != null) {
        long t = clock.getTick() - context.getStartTime();
        timer.update(t, TimeUnit.NANOSECONDS);
      }
    }

    private static OperationId lastId() {
      Deque<OperationContext> contexts = CONTEXT_STACK.get();
      OperationContext prevContext = null;
      Iterator<OperationContext> iterator = contexts.descendingIterator();
      while (iterator.hasNext()){
        prevContext = iterator.next();
        if (prevContext != RENTRANT) {
          break;
        }
      }
      return prevContext != null ? prevContext.getId() : null;
    }
  }

  private static class OperationContext {

    private final OperationId id;
    private final long startTime;

    public OperationContext(OperationId id, Clock clock) {
      this.id = id;
      this.startTime = clock.getTick();
    }

    public OperationId getId() {
      return id;
    }

    public long getStartTime() {
      return startTime;
    }
  }
}
