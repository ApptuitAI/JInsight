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

package ai.apptuit.metrics.jinsight.modules.whalinmemcached;

import ai.apptuit.metrics.jinsight.modules.common.AbstractRuleSet;
import com.whalin.MemCached.MemCachedClient;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Rajiv Shivane
 */
public class WhalinmemcachedRuleSet extends AbstractRuleSet {

  private static final String HELPER_NAME =
      "ai.apptuit.metrics.jinsight.modules.whalinmemcached.WhalinmemcachedRuleHelper";

  private static final String[] instrumentedMethods = new String[]{
      "get", "set", "delete", "add", "replace", "append", "prepend", "gets", "cas", "addOrIncr",
      "incr", "addOrDecr", "decr"
  };

  private final List<RuleInfo> rules = new ArrayList<>();

  public WhalinmemcachedRuleSet() {
    for (String method : instrumentedMethods) {
      addRulesForOperation(method);
    }
  }

  private void addRulesForOperation(String methodName) {
    addRule(MemCachedClient.class, methodName, "AT ENTRY",
        "onOperationStart(\"" + methodName + "\", $0)");
    addRule(MemCachedClient.class, methodName, "AT EXIT",
        "onOperationEnd(\"" + methodName + "\", $0)");
    addRule(MemCachedClient.class, methodName, "AT EXCEPTION EXIT",
        "onOperationError(\"" + methodName + "\", $0)");
  }

  private void addRule(Class clazz, String methodName, String whereClause, String action) {
    String ruleName = clazz + " " + methodName + " " + whereClause;
    RuleInfo rule = new RuleInfo(ruleName, clazz.getName(), clazz.isInterface(), false,
        methodName, HELPER_NAME, whereClause, null, null, action, null, null);
    rules.add(rule);
  }

  @Override
  public List<RuleInfo> getRules() {
    return rules;
  }
}
