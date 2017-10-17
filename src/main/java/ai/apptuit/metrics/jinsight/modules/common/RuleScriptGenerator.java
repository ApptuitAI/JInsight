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

import ai.apptuit.metrics.jinsight.modules.common.AbstractRuleSet.RuleInfo;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Rajiv Shivane
 */
public class RuleScriptGenerator {

  private final List<AbstractRuleSet> ruleSets = new ArrayList<>();

  public RuleScriptGenerator(List<String> helpers) {
    for (String helper : helpers) {
      try {
        Class<?> clazz = Class.forName(helper);
        int modifiers = clazz.getModifiers();
        if (Modifier.isAbstract(modifiers) || Modifier.isInterface(modifiers)) {
          continue;
        }
        if (!AbstractRuleSet.class.isAssignableFrom(clazz)) {
          System.err.println("Not a RuleSet: " + clazz);
          continue;
        }
        AbstractRuleSet ruleSet = (AbstractRuleSet) clazz.newInstance();
        ruleSets.add(ruleSet);
      } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
        System.err.println(e);
      }
    }
  }

  public static void main(String[] args) throws IOException {
    File outputFile = new File(args[0]);
    List<String> helpers = new ArrayList<>(args.length);
    for (int i = 1; i < args.length; i++) {
      String helper = args[i].replaceAll("\\.class$", "")
          .replaceAll("\\\\", ".")
          .replaceAll("/", ".")
          .replaceAll("^\\.", "");
      helpers.add(helper);
    }
    //System.out.println(outputFile);
    //System.out.println(helpers);
    PrintStream ps = new PrintStream(
        new BufferedOutputStream(new FileOutputStream(outputFile, true)));
    new RuleScriptGenerator(helpers).generateRules(ps);
    ps.flush();
    ps.close();
  }

  private void generateRules(PrintStream ps) {
    AtomicInteger total = new AtomicInteger(0);
    ruleSets.forEach(ruleSet -> {
      List<RuleInfo> rules = ruleSet.getRules();
      rules.forEach(ruleInfo -> ruleInfo.generateRule(ps));
      System.out.println(ruleSet.getClass().getSimpleName() + ": " + rules.size());
      total.getAndAdd(rules.size());
    });
    System.out.println("Rules generated: " + total);
  }

}
