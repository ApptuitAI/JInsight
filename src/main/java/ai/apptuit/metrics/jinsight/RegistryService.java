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
import ai.apptuit.metrics.dropwizard.TagEncodedMetricName;
import ai.apptuit.metrics.jinsight.modules.jvm.JvmMetricSet;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import io.prometheus.client.CollectorRegistry;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Provides access to the MetricRegistry that is pre-configured to use {@link ai.apptuit.metrics.dropwizard}. Rest the
 * Agent runtime classes use this registry to create metrics.
 *
 * @author Rajiv Shivane
 */
public class RegistryService {

  private static final Logger LOGGER = Logger.getLogger(RegistryService.class.getName());
  private static final RegistryService singleton = new RegistryService();

  private MetricRegistry registry = new MetricRegistry();
  private Sanitizer sanitizer = null;
  private boolean initialized = false;

  private RegistryService() {
  }

  RegistryService(ConfigService configService, ApptuitReporterFactory factory) {
    initialize(configService, factory);
  }

  boolean initialize() {
    initialize(ConfigService.getInstance(), new ApptuitReporterFactory());
    return initialized;
  }

  private void initialize(ConfigService configService, ApptuitReporterFactory factory) {
    registry.registerAll(new JvmMetricSet());
    String buildInfoMetricName = TagEncodedMetricName.decode("jinsight").submetric("build_info")
        .withTags("version", configService.getAgentVersion()).toString();
    registry.gauge(buildInfoMetricName, () -> () -> 1L);
    MetricRegistryCollection metricRegistryCollection = MetricRegistryCollection.getInstance();
    metricRegistryCollection.initialize(registry);
    startReportingOnRegistryCollection(configService, factory, metricRegistryCollection);
  }

  private void startReportingOnRegistryCollection(ConfigService configService,
      ApptuitReporterFactory factory, MetricRegistryCollection metricRegistryCollection) {

    MetricRegistry aggregatedMetricRegistry = metricRegistryCollection.getAggregatedMetricRegistry();
    ConfigService.ReporterType reporterType = configService.getReporterType();
    if (reporterType == ConfigService.ReporterType.APPTUIT) {
      ReportingMode mode = configService.getReportingMode();
      sanitizer = configService.getSanitizer();
      ScheduledReporter reporter = createReporter(factory, configService.getGlobalTags(),
          configService.getApiToken(), configService.getApiUrl(), mode, aggregatedMetricRegistry);
      reporter.start(configService.getReportingFrequency(), TimeUnit.MILLISECONDS);
      initialized = true;
    } else if (reporterType == ConfigService.ReporterType.PROMETHEUS) {
      CollectorRegistry collectorRegistry = CollectorRegistry.defaultRegistry;
      ApptuitDropwizardExports collector = new ApptuitDropwizardExports(aggregatedMetricRegistry,
          new TagDecodingSampleBuilder(configService.getGlobalTags()));
      collectorRegistry.register(collector);

      try {
        int port = configService.getPrometheusPort();
        InetSocketAddress socket = new InetSocketAddress(port);
        PromHttpServer server = new PromHttpServer(socket, collectorRegistry, true);
        server.setContext(configService.getPrometheusMetricsPath());
        initialized = true;
      } catch (IOException e) {
        LOGGER.log(Level.SEVERE, "Error while creating http port.", e);
      }
    } else {
      throw new IllegalStateException();
    }
  }

  public boolean isInitialized() {
    return initialized;
  }

  public static MetricRegistry getMetricRegistry() {
    RegistryService registryService = getRegistryService();
    if (!registryService.isInitialized()) {
      throw new IllegalStateException("RegistryService not yet initialized.");
    }
    return registryService.registry;
  }

  public static RegistryService getRegistryService() {
    return singleton;
  }

  private ScheduledReporter createReporter(ApptuitReporterFactory factory,
      Map<String, String> globalTags,
      String apiToken, URL apiUrl,
      ReportingMode reportingMode,
      MetricRegistry registry) {
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
