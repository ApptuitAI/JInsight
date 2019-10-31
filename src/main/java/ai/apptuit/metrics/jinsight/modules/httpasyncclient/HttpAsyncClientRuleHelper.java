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

package ai.apptuit.metrics.jinsight.modules.httpasyncclient;

import ai.apptuit.metrics.client.TagEncodedMetricName;
import ai.apptuit.metrics.jinsight.RegistryService;
import ai.apptuit.metrics.jinsight.modules.common.RuleHelper;
import ai.apptuit.metrics.jinsight.modules.httpurlconnection.UrlConnectionRuleHelper;
import com.codahale.metrics.Clock;
import com.codahale.metrics.Timer;
import java.util.concurrent.TimeUnit;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.jboss.byteman.rule.Rule;

/**
 * @author Rajiv Shivane
 */
public class HttpAsyncClientRuleHelper extends RuleHelper {

  public static final TagEncodedMetricName ROOT_NAME = TagEncodedMetricName.decode("http.requests");
  private static final String START_TIME_PROPERTY_NAME =
      UrlConnectionRuleHelper.class + ".START_TIME";
  private static final Clock CLOCK = Clock.defaultClock();

  public HttpAsyncClientRuleHelper(Rule rule) {
    super(rule);
  }


  public void onRequestReady(HttpRequest request) {
    setObjectProperty(request, START_TIME_PROPERTY_NAME, CLOCK.getTick());
  }

  public void onResponseReceived(HttpRequest request, HttpResponse response) {
    Long startTime = removeObjectProperty(request, START_TIME_PROPERTY_NAME);
    if (startTime == null) {
      return;
    }

    long t = Clock.defaultClock().getTick() - startTime;

    String method = request.getRequestLine().getMethod();
    int statusCode = response.getStatusLine().getStatusCode();

    String metricName = ROOT_NAME.withTags(
        "method", method,
        "status", "" + statusCode).toString();
    Timer timer = RegistryService.getMetricRegistry().timer(metricName);
    timer.update(t, TimeUnit.NANOSECONDS);

  }
}
