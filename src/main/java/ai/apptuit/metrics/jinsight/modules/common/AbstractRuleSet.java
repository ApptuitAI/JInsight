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

package ai.apptuit.metrics.jinsight.modules.common;

import java.io.PrintStream;
import java.util.List;

/**
 * @author Rajiv Shivane
 */
public abstract class AbstractRuleSet {

  public abstract List<RuleInfo> getRules();


  public static class RuleInfo {

    public static final String AT_ENTRY = "AT ENTRY";
    public static final String AT_EXIT = "AT EXIT";
    public static final String AT_EXCEPTION_EXIT = "AT EXCEPTION EXIT";
    public static final String CONSTRUCTOR_METHOD = "<init>";
    public static final String CLASS_CONSTRUCTOR = "<clinit>";
    private static final String LINEBREAK = String.format("%n");
    private String ruleName;
    private String className;
    private boolean isInterface;
    private boolean isIncludeSubclases;
    private String methodName;
    private String helperName;
    private String where = AT_ENTRY;
    private String bind;
    private String ifcondition = "true";
    private String action;
    private String imports;
    private String compile;

    public RuleInfo(String ruleName, String className, boolean isInterface,
        boolean isIncludeSubclases, String methodName, String helperName, String where, String bind,
        String ifcondition, String action, String imports, String compile) {
      this.ruleName = ruleName;
      this.className = className;
      this.isInterface = isInterface;
      this.isIncludeSubclases = isIncludeSubclases;
      this.methodName = methodName;
      this.helperName = helperName;
      if (where != null) {
        this.where = where;
      }
      this.bind = bind;
      if (ifcondition != null) {
        this.ifcondition = ifcondition;
      }
      this.action = action;
      this.imports = imports;
      this.compile = compile;
    }

    public String generateRule(PrintStream stringBuilder) {

      stringBuilder.append("RULE ");
      stringBuilder.append(ruleName);
      stringBuilder.append(LINEBREAK);

      if (isInterface) {
        stringBuilder.append("INTERFACE ");
      } else {
        stringBuilder.append("CLASS ");
      }
      if (isIncludeSubclases) {
        stringBuilder.append("^");
      }
      stringBuilder.append(className);
      stringBuilder.append(LINEBREAK);

      stringBuilder.append("METHOD ");
      stringBuilder.append(methodName);
      stringBuilder.append(LINEBREAK);

      stringBuilder.append(where);
      stringBuilder.append(LINEBREAK);

      if (helperName != null) {
        stringBuilder.append("HELPER ");
        stringBuilder.append(helperName);
        stringBuilder.append(LINEBREAK);
      }

      if (imports != null) {
        stringBuilder.append(imports);
      }

      if (compile != null) {
        stringBuilder.append(compile);
        stringBuilder.append(LINEBREAK);
      }

      if (bind != null) {
        stringBuilder.append("BIND ");
        stringBuilder.append(bind);
        stringBuilder.append(LINEBREAK);
      }

      stringBuilder.append("IF ");
      stringBuilder.append(ifcondition);
      stringBuilder.append(LINEBREAK);

      stringBuilder.append("DO ");
      stringBuilder.append(action);
      stringBuilder.append(LINEBREAK);

      stringBuilder.append("ENDRULE");
      stringBuilder.append(LINEBREAK);

      return stringBuilder.toString();
    }
  }
}