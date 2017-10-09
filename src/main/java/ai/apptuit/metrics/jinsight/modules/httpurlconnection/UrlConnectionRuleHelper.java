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

package ai.apptuit.metrics.jinsight.modules.httpurlconnection;

import ai.apptuit.metrics.dropwizard.TagEncodedMetricName;
import ai.apptuit.metrics.jinsight.RegistryService;
import ai.apptuit.metrics.jinsight.modules.common.RuleHelper;
import com.codahale.metrics.Clock;
import com.codahale.metrics.Timer;
import java.net.HttpURLConnection;
import java.util.concurrent.TimeUnit;
import org.jboss.byteman.rule.Rule;

/**
 * @author Rajiv Shivane
 */
public class UrlConnectionRuleHelper extends RuleHelper {

  public static final TagEncodedMetricName ROOT_NAME = TagEncodedMetricName.decode("http.requests");
  private static final String START_TIME_PROPERTY_NAME =
      UrlConnectionRuleHelper.class + ".START_TIME";
  private static final Clock CLOCK = Clock.defaultClock();

  public UrlConnectionRuleHelper(Rule rule) {
    super(rule);
  }

  public void onConnect(HttpURLConnection urlConnection) {
    setObjectProperty(urlConnection, START_TIME_PROPERTY_NAME, CLOCK.getTick());
  }

  public void onGetInputStream(HttpURLConnection urlConnection, int statusCode) {
    Long startTime = removeObjectProperty(urlConnection, START_TIME_PROPERTY_NAME);
    if (startTime == null) {
      return;
    }

    long t = Clock.defaultClock().getTick() - startTime;
    String metricName = ROOT_NAME.withTags(
        "method", urlConnection.getRequestMethod(),
        "status", "" + statusCode).toString();
    Timer timer = RegistryService.getMetricRegistry().timer(metricName);
    timer.update(t, TimeUnit.NANOSECONDS);

  }
}
