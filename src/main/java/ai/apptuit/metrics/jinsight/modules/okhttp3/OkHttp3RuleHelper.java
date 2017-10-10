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

package ai.apptuit.metrics.jinsight.modules.okhttp3;

import ai.apptuit.metrics.dropwizard.TagEncodedMetricName;
import ai.apptuit.metrics.jinsight.modules.common.RuleHelper;
import com.codahale.metrics.Timer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import okhttp3.Request;
import okhttp3.Response;
import org.jboss.byteman.rule.Rule;

/**
 * @author Rajiv Shivane
 */
public class OkHttp3RuleHelper extends RuleHelper {

  public static final TagEncodedMetricName ROOT_NAME = TagEncodedMetricName.decode("http.requests");

  private static final OperationId EXECUTE_OPERATION = new OperationId("okhttp3.execute");

  private Map<String, Timer> timers = new ConcurrentHashMap<>();

  public OkHttp3RuleHelper(Rule rule) {
    super(rule);
  }

  public void onExecuteStart() {
    beginTimedOperation(EXECUTE_OPERATION);
  }

  public void onExecuteException() {
    endTimedOperation(EXECUTE_OPERATION, (Timer) null);
  }

  public void onExecuteEnd(Request request, Response response) {
    endTimedOperation(EXECUTE_OPERATION, () -> {
      String method = request.method();
      String status = "" + response.code();
      return timers.computeIfAbsent(status + method, s -> {
        TagEncodedMetricName metricName = ROOT_NAME.withTags(
            "method", method,
            "status", status);
        return getTimer(metricName);
      });
    });
  }
}
