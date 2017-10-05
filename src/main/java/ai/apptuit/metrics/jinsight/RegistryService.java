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

import ai.apptuit.metrics.dropwizard.ApptuitReporter.ReportingMode;
import ai.apptuit.metrics.dropwizard.ApptuitReporterFactory;
import ai.apptuit.metrics.jinsight.modules.jvm.JvmMetricSet;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Provides access to the MetricRegistry that is pre-configured to use {@link
 * ai.apptuit.metrics.dropwizard.ApptuitReporter}. Rest the Agent runtime classes use this registry
 * to create metrics.
 *
 * @author Rajiv Shivane
 */
public class RegistryService {

  private static final RegistryService singleton = new RegistryService();
  private MetricRegistry registry = null;

  private RegistryService() {
    this.registry = new MetricRegistry();

    String hostname;
    try {
      hostname = InetAddress.getLocalHost().getCanonicalHostName();
    } catch (UnknownHostException e) {
      hostname = "?";
    }

    ConfigService configService = ConfigService.getInstance();

    ReportingMode mode = null;
    String configMode = configService.getReportingMode();
    if (configMode != null) {
      try {
        mode = ReportingMode.valueOf(configMode);
      } catch (IllegalArgumentException e) {
        //TODO Log bad configuration option
        //e.printStackTrace();
      }
    }

    ScheduledReporter reporter = getScheduledReporter(hostname, configService.getGlobalTags(),
        configService.getApiToken(), configService.getApiUrl(), mode);
    reporter.start(5, TimeUnit.SECONDS);

    registry.registerAll(new JvmMetricSet());
  }

  public static MetricRegistry getMetricRegistry() {
    return getRegistryService().registry;
  }

  public static RegistryService getRegistryService() {
    return singleton;
  }

  private ScheduledReporter getScheduledReporter(String hostname, Map<String, String> globalTags,
      String apiToken, String apiUrl, ReportingMode reportingMode) {
    ApptuitReporterFactory factory = new ApptuitReporterFactory();
    factory.setRateUnit(TimeUnit.SECONDS);
    factory.setDurationUnit(TimeUnit.MILLISECONDS);
    factory.addGlobalTag("host", hostname);
    globalTags.forEach(factory::addGlobalTag);
    factory.setApiKey(apiToken);
    factory.setApiUrl(apiUrl);
    factory.setReportingMode(reportingMode);

    return factory.build(registry);
  }
}
