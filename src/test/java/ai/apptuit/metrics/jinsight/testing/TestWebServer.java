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

package ai.apptuit.metrics.jinsight.testing;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.BindException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * @author Rajiv Shivane
 */
public class TestWebServer {

  private static final String ECHO_ENDPOINT = "/echo";
  private HttpServer httpServer = HttpServer.create();

  public TestWebServer() throws IOException {

    int port = 9000;
    boolean bound = false;
    do {
      try {
        httpServer.bind(new InetSocketAddress(port), 0);
        bound = true;
      } catch (BindException e) {
        port++;
      }
    } while (!bound && port < 12000);

    httpServer.createContext(ECHO_ENDPOINT, new EchoHandler());

    httpServer.start();
  }

  public static String streamToString(InputStream inputStream) throws IOException {
    if (inputStream == null) {
      return null;
    }
    try {
      return new Scanner(inputStream).useDelimiter("\0").next();
    } catch (NoSuchElementException e) {
      return null;
    }
  }

  public String getEchoEndpoint() {
    return getEchoEndpoint(HttpURLConnection.HTTP_OK);
  }

  public String getEchoEndpoint(int statusCode) {
    String serverURL = "http://localhost:" + httpServer.getAddress().getPort() + ECHO_ENDPOINT;
    if (statusCode != HttpURLConnection.HTTP_OK && (statusCode > 199) && (statusCode < 599)) {
      serverURL += "?status=" + statusCode;
    }
    return serverURL;
  }

  private Map<String, String> getQueryParameters(HttpExchange httpExchange) {
    String query = httpExchange.getRequestURI().getQuery();
    if (query == null) {
      return Collections.emptyMap();
    }
    Map<String, String> params = new HashMap<>();
    for (String param : query.split("&")) {
      String pair[] = param.split("=");
      if (pair.length > 1) {
        params.put(pair[0], pair[1]);
      } else {
        params.put(pair[0], "");
      }
    }
    return params;
  }

  private class EchoHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
      Map<String, String> queryParameters = getQueryParameters(exchange);
      String statusParam = queryParameters.get("status");
      int statusCode =
          (statusParam != null) ? Integer.parseInt(statusParam) : HttpURLConnection.HTTP_OK;

      String requestBody = streamToString(exchange.getRequestBody());

      String body = exchange.getRequestURI().toString();
      body += "\n";
      body += "\n";
      body += requestBody;

      byte[] response = body.getBytes();
      if (statusCode == HttpURLConnection.HTTP_MOVED_TEMP) {
        String redirectLocation = queryParameters.get("rd");
        if (redirectLocation == null) {
          statusCode = HttpURLConnection.HTTP_BAD_REQUEST;
        } else {
          exchange.getResponseHeaders().add("Location", redirectLocation);
        }
      }
      exchange.sendResponseHeaders(statusCode, response.length);
      OutputStream outputStream = exchange.getResponseBody();
      outputStream.write(response);
      outputStream.close();
      exchange.close();
    }
  }
}
