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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.prometheus.client.Collector.MetricFamilySamples;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.exporter.common.TextFormat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Set;
import java.util.zip.GZIPOutputStream;


public class PromHttpServer extends HTTPServer {

  @SuppressWarnings("squid:S5164")
  private static final ThreadLocal<ByteArrayOutputStream> THREAD_LOCAL_BUFFER = ThreadLocal
      .withInitial(() -> new ByteArrayOutputStream(1 << 20));

  private CollectorRegistry registry;

  /**
   * constructor for the PromHttpServer.
   */

  public PromHttpServer(InetSocketAddress address,
                        CollectorRegistry registry,
                        boolean daemon) throws IOException {
    super(address, registry, daemon);
    this.registry = registry;
    this.server.removeContext("/");
    this.server.removeContext("/metrics");
  }

  /**
   * to set the context(endpoint).
   *
   * @param endPoint it should be a string
   *                 returns the endPoint which is set
   */
  public String setContext(String endPoint) {
    HttpHandler mHandler = new HttpMetricHandler(this.registry);
    String tempEndPoint = endPoint;

    if (endPoint == null || endPoint.equals("")) {
      tempEndPoint = "/metrics"; //default end point
    }
    this.server.createContext(tempEndPoint, mHandler);
    return tempEndPoint;
  }

  private static class HttpMetricHandler implements HttpHandler {

    private CollectorRegistry registry;

    HttpMetricHandler(CollectorRegistry registry) {
      this.registry = registry;
    }


    public void handle(HttpExchange t) throws IOException {
      String query = t.getRequestURI().getRawQuery();

      ByteArrayOutputStream baos = THREAD_LOCAL_BUFFER.get();
      baos.reset();
      OutputStreamWriter osw = new OutputStreamWriter(baos, StandardCharsets.UTF_8);

      Set<String> includedNames = parseQuery(query);
      Enumeration<MetricFamilySamples> mfs;
      if(includedNames.isEmpty()) {
        mfs = registry.metricFamilySamples();
      }else {
        mfs = registry.filteredMetricFamilySamples(includedNames);
      }
      TextFormat.write004(osw, mfs);
      osw.flush();
      osw.close();
      baos.flush();
      baos.close();

      t.getResponseHeaders().set("Content-Type", TextFormat.CONTENT_TYPE_004);
      if (shouldUseCompression(t)) {
        t.getResponseHeaders().set("Content-Encoding", "gzip");
        t.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
        final GZIPOutputStream os = new GZIPOutputStream(t.getResponseBody());
        baos.writeTo(os);
        os.close();
      } else {
        t.getResponseHeaders().set("Content-Length", String.valueOf(baos.size()));
        t.sendResponseHeaders(HttpURLConnection.HTTP_OK, baos.size());
        baos.writeTo(t.getResponseBody());
      }
      t.close();
    }
  }

}

