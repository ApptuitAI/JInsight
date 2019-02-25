package ai.apptuit.metrics.jinsight;

import static ai.apptuit.metrics.jinsight.ConfigService.PROMETHEUS_EXPORTER_PORT;
import static ai.apptuit.metrics.jinsight.ConfigService.REPORTING_MODE_PROPERTY_NAME;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import ai.apptuit.metrics.dropwizard.ApptuitReporterFactory;
import com.codahale.metrics.*;

import java.util.Properties;
import java.util.Random;
import org.junit.Before;
import org.junit.Test;

public class PrometheusTest {
  private ApptuitReporterFactory mockFactory;
  private ConfigService mockConfigService;
  private PromHttpServer mockServer;
  @Before
  public void setUp() throws Exception {
    mockFactory = mock(ApptuitReporterFactory.class);
    mockConfigService = mock(ConfigService.class);
    mockServer = mock(PromHttpServer.class);
    when(mockConfigService.getGlobalTags()).thenReturn(ConfigService.getInstance().getGlobalTags());
  }

  @Test
  public void testGetReportingModePrometheus() throws Exception {
    Properties p = new Properties();
    p.setProperty(REPORTING_MODE_PROPERTY_NAME, "PROMETHEUS");
    ConfigService configService = new ConfigService(p);
    assertEquals(null, configService.getReportingMode());
  }

  @Test
  public void testGetPrometheusPort() throws Exception {
    Properties p = new Properties();
    int port = new Random().nextInt(1000) + 6000;
    p.setProperty(REPORTING_MODE_PROPERTY_NAME, "PROMETHEUS");
    p.setProperty(PROMETHEUS_EXPORTER_PORT, Integer.toString(port));
    ConfigService configService = new ConfigService(p);
    assertEquals(port, configService.getPrometheusPort());
  }

  @Test
  public void testGetPrometheusPortDefault() throws Exception {
    Properties p = new Properties();
    p.setProperty(REPORTING_MODE_PROPERTY_NAME, "PROMETHEUS");
    ConfigService configService = new ConfigService(p);
    //default port 9404
    assertEquals(9404, configService.getPrometheusPort());
  }

  @Test
  public void testGetPrometheusMetricsPathDefault() throws Exception {
    Properties p = new Properties();
    p.setProperty(REPORTING_MODE_PROPERTY_NAME, "PROMETHEUS");
    ConfigService configService = new ConfigService(p);
    //default port 94040
    assertEquals("/metrics", configService.getprometheusMetricsPath());
  }

  @Test
  public void testPrometheusServerCall() throws Exception {
    Properties p = new Properties();
    p.setProperty(REPORTING_MODE_PROPERTY_NAME, "PROMETHEUS");
    ConfigService configService = new ConfigService(p);
    new RegistryService(configService,mockFactory);
    // if no build is called then it is in else block of the RegistryService i.e, promServer
    verify(mockFactory,never()).build(any(MetricRegistry.class));
  }

}
