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
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.dropwizard.DropwizardExports;
import java.net.InetSocketAddress;
import java.net.URL;
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
    this(ConfigService.getInstance(), new ApptuitReporterFactory());
  }

  RegistryService(ConfigService configService, ApptuitReporterFactory factory) {
    this.registry = new MetricRegistry();
    ReportingMode mode = configService.getReportingMode();
    //after update to the AgentReporter use
    //if(mode != ReportingMode.PROMETHEUS)
    if (!configService.getisReportingModePrometheus()) {
      ScheduledReporter reporter = createReporter(factory, configService.getGlobalTags(),
              configService.getApiToken(), configService.getApiUrl(), mode);
      reporter.start(configService.getReportingFrequency(), TimeUnit.MILLISECONDS);
      reporter.report();
    } else {
      DropwizardExports collector = new DropwizardExports(registry, new TomCatSampleBuilder());
      CollectorRegistry.defaultRegistry.register(collector);
      try {
        int port = configService.getReportingPort();
        InetSocketAddress socket = new InetSocketAddress(port);

        // Review running as deamon
        PromHttpServer server = new PromHttpServer(socket, CollectorRegistry.defaultRegistry, true);
        server.setContext(configService.getPrometheusExporterEndPoint());
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    registry.registerAll(new JvmMetricSet());
  }

  public static MetricRegistry getMetricRegistry() {
    return getRegistryService().registry;
  }

  public static RegistryService getRegistryService() {
    return singleton;
  }

  private ScheduledReporter createReporter(ApptuitReporterFactory factory,
                                           Map<String, String> globalTags,
                                           String apiToken, URL apiUrl,
                                           ReportingMode reportingMode) {
    factory.setRateUnit(TimeUnit.SECONDS);
    factory.setDurationUnit(TimeUnit.MILLISECONDS);
    globalTags.forEach(factory::addGlobalTag);
    factory.setApiKey(apiToken);
    factory.setApiUrl(apiUrl != null ? apiUrl.toString() : null);
    factory.setReportingMode(reportingMode);

    return factory.build(registry);
  }
}
