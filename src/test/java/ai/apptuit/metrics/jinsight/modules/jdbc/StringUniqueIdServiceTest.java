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

package ai.apptuit.metrics.jinsight.modules.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;


/**
 * @author Rajiv Shivane
 */
public class StringUniqueIdServiceTest {


  private StringUniqueIdService service = null;

  @Before
  public void setUp() throws Exception {
    service = new StringUniqueIdService();
  }

  @Test
  public void testNonCached() throws Exception {
    String query = "SELECT * FROM TEST where ID=?";
    String underlyingId = service.getIdFromServer(query);
    String clientId = service.getUniqueId(query);
    assertEquals(underlyingId, clientId);
  }

  @Test
  public void testCached() throws Exception {
    String query = "SELECT * FROM TEST where USER=?";
    String underlyingId = service.getIdFromServer(query);
    String clientId = service.getUniqueId(query);
    assertEquals(underlyingId, clientId);
    String cachedId = service.getUniqueId(query);
    assertEquals(clientId, cachedId);
  }

  @Test
  public void testNull() throws Exception {
    String cachedId = service.getUniqueId(null);
    assertNull(cachedId);
  }
}
