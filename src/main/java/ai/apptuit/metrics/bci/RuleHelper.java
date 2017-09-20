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

package ai.apptuit.metrics.bci;

import ai.apptuit.metrics.dropwizard.ApptuitReporterFactory;
import ai.apptuit.metrics.dropwizard.TagEncodedMetricName;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Timer.Context;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Stack;
import java.util.concurrent.TimeUnit;
import org.jboss.byteman.rule.Rule;
import org.jboss.byteman.rule.helper.Helper;

/**
 * @author Rajiv Shivane
 */
public class RuleHelper extends Helper {

  private static final String APPTUIT_API_KEY = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9."
      + "eyJzdWIiOiJwbGF5Z3JvdW5kIiwiaXNzIjoiYXBwdHVpdCIsIlRFTkFOVCI6ImMwYTZiNmFmLWU4M"
      + "zAtNDhhMS1hMGE4LTRhNDBlYThhZTE5MyIsImlhdCI6MTUwMzMwNTI4OCwianRpIjoiMSJ9.vu88X"
      + "K7MbpMJhwszWMaYZHgGVRsrCElPkiNL7eMBac0";

  protected static final MetricRegistry registry = new MetricRegistry();

  static {

    String hostname;
    try {
      hostname = InetAddress.getLocalHost().getCanonicalHostName();
    } catch (UnknownHostException e) {
      hostname = "?";
    }

    ApptuitReporterFactory factory = new ApptuitReporterFactory();
    factory.setRateUnit(TimeUnit.SECONDS);
    factory.setDurationUnit(TimeUnit.MILLISECONDS);
    factory.addGlobalTag("hostname", hostname);
    factory.setApiKey(APPTUIT_API_KEY);

    ScheduledReporter reporter = factory.build(registry);
    reporter.start(5, TimeUnit.SECONDS);

    /*
    final ConsoleReporter consoleReporter = ConsoleReporter.forRegistry(registry)
        .convertRatesTo(TimeUnit.SECONDS)
        .convertDurationsTo(TimeUnit.MILLISECONDS)
        .build();
    consoleReporter.start(5, TimeUnit.SECONDS);
    */
  }

  public RuleHelper(Rule rule) {
    super(rule);
  }

  public void startTimer(TagEncodedMetricName metric) {
    Timers.start(metric);
  }

  public void stopTimer(TagEncodedMetricName metric) {
    Timers.stop(metric);
  }

  private static class Timers {

    private static final ThreadLocal<Stack<Context>> TIMERS = ThreadLocal.withInitial(Stack::new);

    public static void start(TagEncodedMetricName metric) {
      TIMERS.get().push(registry.timer(metric.toString()).time());
    }

    public static void stop(TagEncodedMetricName metric) {
      Context context = TIMERS.get().pop();
      long t = context.stop();
      //TODO verify the Context we popped is the one for the same metric
      //System.out.printf("Done in [%d] nanos\n", t);
    }
  }
}
