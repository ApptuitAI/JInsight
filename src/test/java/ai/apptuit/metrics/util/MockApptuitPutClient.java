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

package ai.apptuit.metrics.util;

import static org.mockito.Matchers.anyCollectionOf;
import static org.mockito.Mockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.mock;

import ai.apptuit.metrics.client.ApptuitPutClient;
import ai.apptuit.metrics.client.DataPoint;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;

/**
 * @author Rajiv Shivane
 */
public class MockApptuitPutClient {

  private static final MockApptuitPutClient instance = new MockApptuitPutClient();

  static {
    try {
      initialize();
    } catch (Exception e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  private MockApptuitPutClient() {
  }

  public static MockApptuitPutClient getInstance() {
    return instance;
  }

  public static void initialize() throws Exception {
    ApptuitPutClient mockPutClient = mock(ApptuitPutClient.class);
    PowerMockito.whenNew(ApptuitPutClient.class).withAnyArguments().thenReturn(mockPutClient);

    doAnswer((Answer<Void>) invocation -> {
      Object[] args = invocation.getArguments();
      listeners.forEach(listener -> listener.onPut((Collection<DataPoint>)args[0]));
      return null;
    }).when(mockPutClient).put(anyCollectionOf(DataPoint.class));

  }

  private static  final List<PutListener> listeners = new ArrayList<>();

  public void addPutListener(PutListener listener){
    listeners.add(listener);
  }

  public boolean removePutListener(PutListener listener){
    return listeners.remove(listener);
  }

  public static interface PutListener{
    public void onPut(Collection<DataPoint> dataPoints);
  }

}
