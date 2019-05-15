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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.function.BiConsumer;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jboss.byteman.agent.Main;

/**
 * The main agent class for JInsight. A wrapper around ByteMan Agent to easy configuration and setup
 *
 * @author Rajiv Shivane
 */
public class Agent {

  private static final Logger LOGGER = Logger.getLogger(Agent.class.getName());

  private static final String BYTEMAN_JAR_RESOURCE_NAME = "META-INF/boot/byteman.jar";
  private static final String BTM_SCRIPTS_RESOURCE_PATH = "META-INF/btm-scripts/jinsight.btm";
  private static final String MODULE_LOADER_CLASSNAME =
      "ai.apptuit.metrics.jinsight.ContextualModuleLoader";

  private static final String AGENT_PARAMS = "resourcescript:" + BTM_SCRIPTS_RESOURCE_PATH
      + ",modules:" + MODULE_LOADER_CLASSNAME;

  public static void premain(String agentArgs, Instrumentation instrumentation) throws Exception {
    main0(agentArgs, instrumentation, (a, i) -> {
      try {
        Main.premain(a, i);
      } catch (Exception e) {
        throw new AgentInitializationException(e);
      }
    });
  }

  public static void agentmain(String agentArgs, Instrumentation instrumentation) throws Exception {
    main0(agentArgs, instrumentation, (a, i) -> {
      try {
        Main.agentmain(a, i);
      } catch (Exception e) {
        throw new AgentInitializationException(e);
      }
    });
  }

  private static void main0(String agentArgs, Instrumentation instrumentation,
      BiConsumer<String, Instrumentation> delegate) {
    if (ClassLoader.getSystemResource(BTM_SCRIPTS_RESOURCE_PATH) == null) {
      LOGGER.severe("Could not load " + BTM_SCRIPTS_RESOURCE_PATH + "."
          + "Agent will not be started.");
      return;
    }

    try {
      ConfigService.initialize();
    } catch (ConfigurationException | IOException | RuntimeException e) {
      LOGGER.severe(e.getMessage());
      LOGGER.severe("Agent will not be started.");
      return;
    }

    if (agentArgs != null && agentArgs.trim().length() > 0) {
      agentArgs += "," + AGENT_PARAMS;
    } else {
      agentArgs = AGENT_PARAMS;
    }

    JarFile bytemanJar = createBytemanJar();
    if (bytemanJar == null) {
      LOGGER.severe("Could not locate: " + BYTEMAN_JAR_RESOURCE_NAME);
      LOGGER.severe("Agent will not be started.");
      return;
    }

    instrumentation.appendToBootstrapClassLoaderSearch(bytemanJar);
    delegate.accept(agentArgs, instrumentation);
    ConfigService configService = ConfigService.getInstance();
    boolean success = RegistryService.getRegistryService().initialize();
    LOGGER.info(getStartupMessage(configService, success));
  }

  public static String getStartupMessage(ConfigService configService, boolean initialized) {
    ConfigService.ReporterType reporterType = configService.getReporterType();
    StringBuilder infoMessage = new StringBuilder();
    infoMessage.append(String.format("Initializing JInsight v[%s] with config [reporter: %s] ",
        configService.getAgentVersion(), reporterType));
    if (reporterType == ConfigService.ReporterType.APPTUIT) {
      infoMessage.append(String.format("[mode: %s ] [frequency: %s seconds]",
          configService.getReportingMode(), (configService.getReportingFrequency() / 1000)));
    } else if (reporterType == ConfigService.ReporterType.PROMETHEUS) {
      infoMessage.append(String.format("[port: %s] [metrics-path: %s ]",
          configService.getPrometheusPort(), configService.getPrometheusMetricsPath()));
    } else {
      throw new IllegalStateException();
    }
    infoMessage.append(initialized ? ": SUCCESSFUL" : ": FAILED");
    return infoMessage.toString();
  }

  private static JarFile createBytemanJar() {
    ClassLoader classLoader = Agent.class.getClassLoader();
    InputStream bytemanStream = classLoader.getResourceAsStream(BYTEMAN_JAR_RESOURCE_NAME);
    if (bytemanStream == null) {
      return null;
    }
    try {
      //TODO Improve. Can we skip extraction if the hash matches?
      File bytemanJar = File.createTempFile("byteman", ".jar");
      bytemanJar.deleteOnExit();
      Files.copy(bytemanStream, bytemanJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
      return new JarFile(bytemanJar);
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Error extracting byteman.jar.", e);
    }
    return null;
  }

  private static class AgentInitializationException extends RuntimeException {

    public AgentInitializationException(Exception e) {
      super(e);
    }
  }
}
