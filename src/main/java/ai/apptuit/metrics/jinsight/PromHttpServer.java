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
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.exporter.common.TextFormat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.util.zip.GZIPOutputStream;

/*
 * helps to create server and remove the unnecessary end points of a port
 */
public class PromHttpServer  extends HTTPServer {
  private CollectorRegistry registry;

  private static class LocalByteArray extends ThreadLocal<ByteArrayOutputStream> {
    protected ByteArrayOutputStream initialValue() {
      return new ByteArrayOutputStream(1 << 20);
    }
  }

  class HttpMetricHandler implements HttpHandler {

    private CollectorRegistry registry;
    private final LocalByteArray response = new LocalByteArray();

    HttpMetricHandler(CollectorRegistry registry) {
      this.registry = registry;
    }


    public void handle(HttpExchange t) throws IOException {
      String query = t.getRequestURI().getRawQuery();

      ByteArrayOutputStream response = this.response.get();
      response.reset();
      OutputStreamWriter osw = new OutputStreamWriter(response);
      TextFormat.write004(osw,
              registry.filteredMetricFamilySamples(parseQuery(query)));
      osw.flush();
      osw.close();
      response.flush();
      response.close();

      t.getResponseHeaders().set("Content-Type",
              TextFormat.CONTENT_TYPE_004);
      if (shouldUseCompression(t)) {
        t.getResponseHeaders().set("Content-Encoding", "gzip");
        t.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
        final GZIPOutputStream os = new GZIPOutputStream(t.getResponseBody());
        response.writeTo(os);
        os.close();
      } else {
        t.getResponseHeaders().set("Content-Length",
                String.valueOf(response.size()));
        t.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.size());
        response.writeTo(t.getResponseBody());
      }
      t.close();
    }
  }

  public PromHttpServer(InetSocketAddress socket,
                        CollectorRegistry registry,
                        boolean deamon) throws IOException {
    super(socket, registry, deamon);
    this.registry = registry;
    this.server.removeContext("/");
    this.server.removeContext("/metrics");
  }

  public void setContext(String endPoint) {
    HttpHandler mHandler = new HttpMetricHandler(this.registry);
    if (endPoint == null) {
      endPoint = "/metrics"; //default end point
    }
    this.server.createContext(endPoint, mHandler);
  }
}

