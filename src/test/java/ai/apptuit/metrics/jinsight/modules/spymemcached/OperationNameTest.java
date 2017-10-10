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

package ai.apptuit.metrics.jinsight.modules.spymemcached;

import static org.junit.Assert.assertEquals;

import ai.apptuit.metrics.jinsight.modules.spymemcached.SpymemcachedRuleHelper.Operations;
import net.spy.memcached.protocol.binary.GetAndTouchOperationImpl;
import net.spy.memcached.protocol.binary.SASLStepOperationImpl;
import net.spy.memcached.protocol.binary.StatsOperationImpl;
import net.spy.memcached.protocol.binary.TapBackfillOperationImpl;
import org.junit.Test;

/**
 * @author Rajiv Shivane
 */
public class OperationNameTest {

  @Test
  public void testSimple() throws Exception {
    String name = Operations.getOperationName(StatsOperationImpl.class);
    assertEquals("stats", name);
  }

  @Test
  public void testDouble() throws Exception {
    String name = Operations.getOperationName(TapBackfillOperationImpl.class);
    assertEquals("tap_backfill", name);
  }

  @Test
  public void testTriple() throws Exception {
    String name = Operations.getOperationName(GetAndTouchOperationImpl.class);
    assertEquals("get_and_touch", name);
  }

  @Test
  public void testAbbrev() throws Exception {
    String name = Operations.getOperationName(SASLStepOperationImpl.class);
    assertEquals("sasl_step", name);
  }
}
