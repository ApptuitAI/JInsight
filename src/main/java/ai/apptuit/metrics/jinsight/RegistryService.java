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

package ai.apptuit.metrics.jinsight;

import ai.apptuit.metrics.dropwizard.ApptuitReporterFactory;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

/**
 * Provides access to the MetricRegistry that is pre-configured to use {@link
 * ai.apptuit.metrics.dropwizard.ApptuitReporter}. Rest the Agent runtime classes use this registry
 * to create metrics.
 *
 * @author Rajiv Shivane
 */
public class RegistryService {

  private static RegistryService singleton = null;
  private MetricRegistry registry = null;

  private RegistryService(MetricRegistry registry) {
    this.registry = registry;
  }

  public static RegistryService getRegistryService() {
    if (singleton != null) {
      return singleton;
    }

    synchronized (RegistryService.class) {
      if (singleton != null) {
        return singleton;
      }
      initialize(ConfigService.getInstance().getApiToken());
    }
    return singleton;
  }

  public static void initialize(String apiToken) {

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
    factory.setApiKey(apiToken);

    MetricRegistry metricRegistry = new MetricRegistry();
    ScheduledReporter reporter = factory.build(metricRegistry);
    reporter.start(5, TimeUnit.SECONDS);

    /*
    final ConsoleReporter consoleReporter = ConsoleReporter.forRegistry(registry)
        .convertRatesTo(TimeUnit.SECONDS)
        .convertDurationsTo(TimeUnit.MILLISECONDS)
        .build();
    consoleReporter.start(5, TimeUnit.SECONDS);
    */

    singleton = new RegistryService(metricRegistry);
  }

  public static MetricRegistry getMetricRegistry() {
    return getRegistryService().registry;
  }

}
