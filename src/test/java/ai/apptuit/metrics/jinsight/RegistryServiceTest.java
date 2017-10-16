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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.apptuit.metrics.dropwizard.ApptuitReporter.ReportingMode;
import ai.apptuit.metrics.dropwizard.ApptuitReporterFactory;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Rajiv Shivane
 */

public class RegistryServiceTest {

  private ApptuitReporterFactory mockFactory;
  private ConfigService mockConfigService;

  @Before
  public void setUp() throws Exception {
    mockFactory = mock(ApptuitReporterFactory.class);
    when(mockFactory.build(any(MetricRegistry.class))).thenReturn(mock(ScheduledReporter.class));
    mockConfigService = mock(ConfigService.class);
    when(mockConfigService.getGlobalTags()).thenReturn(ConfigService.getInstance().getGlobalTags());
  }

  @Test
  public void testAcessToken() throws Exception {
    String token = UUID.randomUUID().toString();
    when(mockConfigService.getApiToken()).thenReturn(token);

    new RegistryService(mockConfigService, mockFactory);

    verify(mockFactory).setApiKey(token);
  }

  @Test
  public void testApiUrl() throws Exception {
    String apiUrl = "https://localhost.locadomain/api/put?uid=" + UUID.randomUUID().toString();
    when(mockConfigService.getApiUrl()).thenReturn(apiUrl);

    new RegistryService(mockConfigService, mockFactory);

    verify(mockFactory).setApiUrl(apiUrl);
  }


  @Test
  public void testBadApiUrl() throws Exception {
    String apiUrl = UUID.randomUUID().toString();
    when(mockConfigService.getApiUrl()).thenReturn(apiUrl);

    new RegistryService(mockConfigService, mockFactory);

    verify(mockFactory).setApiUrl(null);
  }

  @Test
  public void testGlobalHostTagAlways() throws Exception {
    Map<String, String> globalTags = new HashMap<>();
    when(mockConfigService.getGlobalTags()).thenReturn(globalTags);

    new RegistryService(mockConfigService, mockFactory);

    verify(mockFactory).addGlobalTag(eq("host"), anyString());
  }


  @Test
  public void testGlobalTagsOverrideDefaultHostTag() throws Exception {
    Map<String, String> globalTags = new HashMap<>();
    globalTags.put("host", "override");
    when(mockConfigService.getGlobalTags()).thenReturn(globalTags);

    new RegistryService(mockConfigService, mockFactory);

    verify(mockFactory).addGlobalTag(eq("host"), eq("override"));
    verify(mockFactory, times(1)).addGlobalTag(anyString(), anyString());
  }


  @Test
  public void testGlobalTagsMulti() throws Exception {
    Map<String, String> globalTags = new HashMap<>();
    globalTags.put("k1", "v1");
    globalTags.put("k2", "v2");
    when(mockConfigService.getGlobalTags()).thenReturn(globalTags);

    new RegistryService(mockConfigService, mockFactory);

    verify(mockFactory, times(3)).addGlobalTag(anyString(), anyString());
    verify(mockFactory, times(1)).addGlobalTag(eq("k1"), eq("v1"));
    verify(mockFactory, times(1)).addGlobalTag(eq("k2"), eq("v2"));
    verify(mockFactory, times(1)).addGlobalTag(eq("host"), anyString());
  }

  @Test
  public void testReportingModeNoOp() throws Exception {
    when(mockConfigService.getReportingMode()).thenReturn("NO_OP");

    new RegistryService(mockConfigService, mockFactory);

    verify(mockFactory).setReportingMode(ReportingMode.NO_OP);
  }

  @Test
  public void testReportingModeNoOpCaseInsensitive() throws Exception {
    when(mockConfigService.getReportingMode()).thenReturn("no_op");

    new RegistryService(mockConfigService, mockFactory);

    verify(mockFactory).setReportingMode(ReportingMode.NO_OP);
  }

  @Test
  public void testReportingModeBad() throws Exception {
    when(mockConfigService.getReportingMode()).thenReturn("junk");

    new RegistryService(mockConfigService, mockFactory);

    verify(mockFactory).setReportingMode(null);
  }
}
