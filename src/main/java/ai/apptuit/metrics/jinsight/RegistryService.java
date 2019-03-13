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

import ai.apptuit.metrics.client.Sanitizer;
import ai.apptuit.metrics.dropwizard.ApptuitReporter.ReportingMode;
import ai.apptuit.metrics.dropwizard.ApptuitReporterFactory;
import ai.apptuit.metrics.jinsight.modules.jvm.JvmMetricSet;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import io.prometheus.client.CollectorRegistry;

import java.net.InetSocketAddress;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Provides access to the MetricRegistry that is pre-configured to use {@link
 * ai.apptuit.metrics.dropwizard.ApptuitReporter}. Rest the Agent runtime classes use this registry
 * to create metrics.
 *
 * @author Rajiv Shivane
 */
public class RegistryService {

  private static final Logger LOGGER = Logger.getLogger(RegistryService.class.getName());
  private static final RegistryService singleton = new RegistryService();
  private MetricRegistry registry = null;
  private Sanitizer sanitizer = null;

  private RegistryService() {
    this(ConfigService.getInstance(), new ApptuitReporterFactory());
  }

  RegistryService(ConfigService configService, ApptuitReporterFactory factory) {
    this.registry = new MetricRegistry();
    ConfigService.ReporterType reporterType = configService.getReporterType();

    if (reporterType == ConfigService.ReporterType.APPTUIT) {
      ReportingMode mode = configService.getReportingMode();
      sanitizer = configService.getSanitizer();
      ScheduledReporter reporter = createReporter(factory, configService.getGlobalTags(),
              configService.getApiToken(), configService.getApiUrl(), mode);
      reporter.start(configService.getReportingFrequency(), TimeUnit.MILLISECONDS);
    } else if (reporterType == ConfigService.ReporterType.PROMETHEUS) {
      ApptuitDropwizardExports collector = new ApptuitDropwizardExports(
              registry, new TagDecodingSampleBuilder(configService.getGlobalTags()));
      CollectorRegistry.defaultRegistry.register(collector);

      try {
        int port = configService.getPrometheusPort();
        InetSocketAddress socket = new InetSocketAddress(port);

        // To run server as daemon thread use bool true
        PromHttpServer server = new PromHttpServer(socket, CollectorRegistry.defaultRegistry, true);
        server.setContext(configService.getPrometheusMetricsPath());
      } catch (Exception e) {
        LOGGER.log(Level.SEVERE, "Error while creating http port.", e);
      }
    } else {
      throw new IllegalStateException();
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
    if (sanitizer != null) {
      factory.setSanitizer(sanitizer);
    }
    return factory.build(registry);
  }
}
