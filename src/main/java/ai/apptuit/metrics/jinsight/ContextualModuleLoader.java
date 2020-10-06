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
import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import org.jboss.byteman.modules.ModuleSystem;
import org.jboss.byteman.rule.helper.Helper;

/**
 * @author Rajiv Shivane
 */
public class ContextualModuleLoader implements ModuleSystem<ModuleClassLoader> {

  private final Map<ClassLoader, SoftReference<ModuleClassLoader>> moduleLoaders = Collections
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

    ModuleClassLoader moduleClassLoader = getModuleClassLoader(triggerClassLoader);
    if (moduleClassLoader != null) {
      return moduleClassLoader;
    }
    synchronized (moduleLoaders) {
      //Double check idiom
      moduleClassLoader = getModuleClassLoader(triggerClassLoader);
      if (moduleClassLoader != null) {
        return moduleClassLoader;
      }
      moduleClassLoader = AccessController.doPrivileged(
          (PrivilegedAction<ModuleClassLoader>) () -> new ModuleClassLoader(triggerClassLoader));
      //Since there are no strong references to moduleClassloader, it might be collected
      //We should probably hold a PhantomReference to moduleClassloader
      //that is collected only after the triggerClassloader is collected
      moduleLoaders.put(triggerClassLoader, new SoftReference<>(moduleClassLoader));
      return moduleClassLoader;
    }
  }

  private ModuleClassLoader getModuleClassLoader(ClassLoader triggerClassLoader) {
    SoftReference<ModuleClassLoader> reference = moduleLoaders.get(triggerClassLoader);
    if (reference != null) {
      ModuleClassLoader moduleClassLoader = reference.get();
      if (moduleClassLoader != null) {
        return moduleClassLoader;
      }
    }
    return null;
  }

  public void destroyLoader(ModuleClassLoader helperLoader) {
    moduleLoaders.remove(helperLoader.getParent());
  }

  public Class<?> loadHelperAdapter(ModuleClassLoader helperLoader, String helperAdapterName,
      byte[] classBytes) {
    return helperLoader.addClass(helperAdapterName, classBytes);
  }

  public static class ModuleClassLoader extends URLClassLoader {

    private static final URL[] SYS_CLASS_PATH = getSystemClassPath();

    public ModuleClassLoader(ClassLoader cl) {
      super(SYS_CLASS_PATH, cl);
    }

    private static URL[] getSystemClassPath() {
      String cp = System.getProperty("java.class.path");
      if (cp == null || cp.isEmpty()) {
        String initialModuleName = System.getProperty("jdk.module.main");
        cp = initialModuleName == null ? "" : null;
      }

      ArrayList<URL> path = new ArrayList<>();
      if (cp != null) {
        int off = 0;
        int next;
        do {
          next = cp.indexOf(File.pathSeparator, off);
          String element = next == -1 ? cp.substring(off) : cp.substring(off, next);
          if (!element.isEmpty()) {
            try {
              URL url = (new File(element)).getCanonicalFile().toURI().toURL();
              path.add(url);
            } catch (IOException ignored) {
              //Ignore invalid urls
            }
          }

          off = next + 1;
        } while (next != -1);
      }
      path.add(ContextualModuleLoader.class.getProtectionDomain().getCodeSource().getLocation());
      return path.toArray(new URL[0]);
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
        synchronized (this) {
          clazz = findClass(name);
          resolveClass(clazz);
          return clazz;
        }
      } else if (name.startsWith("ai.apptuit.metrics.jinsight.")) {
        //If jinsight.jar is added to the war, by mistake, we should always go to sys classloader
        return ClassLoader.getSystemClassLoader().loadClass(name);
      }
      return super.loadClass(name);
    }
  }
}
