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

import ai.apptuit.metrics.jinsight.ContextualModuleLoader.ModuleClassLoader;
import java.lang.ref.WeakReference;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import org.jboss.byteman.modules.ModuleSystem;
import org.jboss.byteman.rule.helper.Helper;

/**
 * @author Rajiv Shivane
 */
public class ContextualModuleLoader implements ModuleSystem<ModuleClassLoader> {

  private final Map<ClassLoader, WeakReference<ModuleClassLoader>> moduleLoaders = Collections
      .synchronizedMap(new WeakHashMap<>());

  public void initialize(String args) {
    if (!args.isEmpty()) {
      Helper.err("Unexpected module system arguments: " + args);
    }
  }

  public ModuleClassLoader createLoader(ClassLoader triggerClassLoader, String[] imports) {
    if (imports.length > 0) {
      throw new IllegalArgumentException("IMPORTs are not supported");
    }

    WeakReference<ModuleClassLoader> reference = moduleLoaders
        .computeIfAbsent(triggerClassLoader, cl -> new WeakReference<>(new ModuleClassLoader(cl)));
    return reference.get();
  }

  public void destroyLoader(ModuleClassLoader helperLoader) {
  }

  public Class<?> loadHelperAdapter(ModuleClassLoader helperLoader, String helperAdapterName,
      byte[] classBytes) {
    return helperLoader.addClass(helperAdapterName, classBytes);
  }

  public static class ModuleClassLoader extends URLClassLoader {

    public ModuleClassLoader(ClassLoader cl) {
      super(((URLClassLoader) ClassLoader.getSystemClassLoader()).getURLs(), cl);
    }

    public Class<?> addClass(String name, byte[] bytes)
        throws ClassFormatError {
      Class<?> cl = defineClass(name, bytes, 0, bytes.length);
      resolveClass(cl);

      return cl;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
      if (name.startsWith("ai.apptuit.metrics.jinsight.modules.")) {
        Class<?> clazz = findLoadedClass(name);
        if (clazz != null) {
          return clazz;
        }
        //TODO synchronization
        clazz = findClass(name);
        resolveClass(clazz);
        return clazz;
      } else if (name.startsWith("ai.apptuit") || name.startsWith("com.codahale.metrics")) {
        return ClassLoader.getSystemClassLoader().loadClass(name);
      }

      return super.loadClass(name);
    }
  }
}
