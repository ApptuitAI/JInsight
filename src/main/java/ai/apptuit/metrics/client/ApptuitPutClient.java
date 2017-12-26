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

package ai.apptuit.metrics.client;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.GZIPOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Rajiv Shivane
 */
public class ApptuitPutClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(ApptuitPutClient.class);

  private static final boolean DEBUG = true;
  private static final boolean GZIP = true;

  private static final int BUFFER_SIZE = 8 * 1024;
  private static final int MAX_RESP_LENGTH = 5 * 1024 * 1024;
  private static final int CONNECT_TIMEOUT_MS = 5000;
  private static final int SOCKET_TIMEOUT_MS = 15000;

  private static final String CONTENT_TYPE = "Content-Type";
  private static final String APPLICATION_JSON = "application/json";
  private static final String CONTENT_ENCODING = "Content-Encoding";
  private static final String CONTENT_ENCODING_GZIP = "gzip";

  private static final URL DEFAULT_PUT_API_URI;

  static {
    try {
      DEFAULT_PUT_API_URI = new URL("https://api.apptuit.ai/api/put?details");
    } catch (MalformedURLException e) {
      throw new IllegalStateException(e);
    }
  }

  private final URL apiEndPoint;

  private Map<String, String> globalTags;
  private String token;

  public ApptuitPutClient(String token, Map<String, String> globalTags) {
    this(token, globalTags, null);
  }

  public ApptuitPutClient(String token, Map<String, String> globalTags, URL apiEndPoint) {
    this.globalTags = globalTags;
    this.token = token;
    this.apiEndPoint = (apiEndPoint != null) ? apiEndPoint : DEFAULT_PUT_API_URI;
  }

  public void put(Collection<DataPoint> dataPoints) {

    DatapointsHttpEntity entity = new DatapointsHttpEntity(dataPoints, globalTags);

    HttpURLConnection urlConnection;
    int status;
    try {
      urlConnection = (HttpURLConnection) apiEndPoint.openConnection();
      urlConnection.setConnectTimeout(CONNECT_TIMEOUT_MS);
      urlConnection.setReadTimeout(SOCKET_TIMEOUT_MS);
      urlConnection.setChunkedStreamingMode(0);

      urlConnection.setRequestProperty(CONTENT_TYPE, APPLICATION_JSON);
      if (GZIP) {
        urlConnection.setRequestProperty(CONTENT_ENCODING, CONTENT_ENCODING_GZIP);
      }
      urlConnection.setRequestProperty("Authorization", "Bearer " + token);
      urlConnection.setRequestMethod("POST");
      urlConnection.setDoInput(true);
      urlConnection.setDoOutput(true);
      OutputStream outputStream = new BufferedOutputStream(urlConnection.getOutputStream(),
          BUFFER_SIZE);
      entity.writeTo(outputStream);
      outputStream.flush();

      status = urlConnection.getResponseCode();
      debug("-------------------" + status + "---------------------");
    } catch (IOException e) {
      //TODO: Return status to caller, so they can choose to retry etc
      LOGGER.error("Error posting data", e);
      return;
    }

    try {
      InputStream inputStr;
      if (status < HttpURLConnection.HTTP_BAD_REQUEST) {
        inputStr = urlConnection.getInputStream();
      } else {
        /* error from server */
        inputStr = urlConnection.getErrorStream();
      }

      String encoding = urlConnection.getContentEncoding() == null ? "UTF-8"
          : urlConnection.getContentEncoding();
      String responseBody = consumeResponse(inputStr, Charset.forName(encoding));
      debug(responseBody);
    } catch (IOException e) {
      LOGGER.error("Error draining response", e);
    }

  }

  private String consumeResponse(InputStream inputStr, Charset encoding) {
    StringBuilder body = new StringBuilder();
    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStr, encoding));
    try {
      int i;
      char[] cbuf = new char[BUFFER_SIZE];
      while ((i = reader.read(cbuf)) > 0) {
        if (body == null) {
          continue;
        }
        if (body.length() + i >= MAX_RESP_LENGTH) {
          body = null;
        } else {
          body.append(cbuf, 0, i);
        }
      }
    } catch (IOException e) {
      LOGGER.error("Error reading response", e);
    }
    return body == null ? "Response too long" : body.toString();
  }

  private void debug(String s) {
    if (DEBUG) {
      LOGGER.info(s);
    }
  }

  static class DatapointsHttpEntity {

    private final Collection<DataPoint> dataPoints;
    private final Map<String, String> globalTags;
    private final boolean doZip;

    public DatapointsHttpEntity(Collection<DataPoint> dataPoints,
        Map<String, String> globalTags) {
      this(dataPoints, globalTags, GZIP);
    }

    public DatapointsHttpEntity(Collection<DataPoint> dataPoints,
        Map<String, String> globalTags, boolean doZip) {
      this.dataPoints = dataPoints;
      this.globalTags = globalTags;
      this.doZip = doZip;
    }

    public void writeTo(OutputStream outputStream) throws IOException {
      if (doZip) {
        outputStream = new GZIPOutputStream(outputStream);
      }

      PrintStream ps = new PrintStream(outputStream, false, "UTF-8");
      ps.println("[");
      Iterator<DataPoint> iterator = dataPoints.iterator();
      while (iterator.hasNext()) {
        DataPoint dp = iterator.next();
        dp.toJson(ps, globalTags);
        if (iterator.hasNext()) {
          ps.println(",");
        }
      }
      ps.println("]");

      ps.flush();

      if (doZip) {
        ((GZIPOutputStream) outputStream).finish();
      }
    }
  }
}
