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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.GZIPOutputStream;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

/**
 * @author Rajiv Shivane
 */
public class ApptuitPutClient {

  private static final int CONNECT_TIMEOUT_MS = 5000;
  private static final int SOCKET_TIMEOUT_MS = 15000;
  private static final int CONNECTION_REQUEST_TIMEOUT_MS = 1000;
  private static final String PUT_API_URI = "https://api.apptuit.ai/api/put?details";
  private static final boolean GZIP = true;

  private final String apiEndPoint;

  private Map<String, String> globalTags;
  private String token;
  private CloseableHttpClient httpclient;

  public ApptuitPutClient(String token, Map<String, String> globalTags) {
    this(token, globalTags, PUT_API_URI);
  }

  ApptuitPutClient(String token, Map<String, String> globalTags, String apiEndPoint) {
    this.globalTags = globalTags;
    this.token = token;
    this.apiEndPoint = apiEndPoint;

    this.httpclient = HttpClients.createDefault();
  }

  public void put(Collection<DataPoint> dataPoints) {

    DatapointsHttpEntity entity = new DatapointsHttpEntity(dataPoints, globalTags);

    HttpPost httpPost = new HttpPost(apiEndPoint);
    httpPost.setEntity(entity);
    httpPost.setHeader("Authorization", "Bearer " + token);

    httpPost.setConfig(RequestConfig.copy(
        RequestConfig.DEFAULT)
        .setConnectTimeout(CONNECT_TIMEOUT_MS)
        .setSocketTimeout(SOCKET_TIMEOUT_MS)
        .setConnectionRequestTimeout(CONNECTION_REQUEST_TIMEOUT_MS)
        .build()
    );

    try {
      HttpResponse response = httpclient.execute(httpPost);
      int status = response.getStatusLine().getStatusCode();

      System.out.println("-------------------" + status + "---------------------");
      HttpEntity respEntity = response.getEntity();
      String responseBody = respEntity != null ? EntityUtils.toString(respEntity) : null;
      System.out.println(responseBody);
    } catch (IOException e) {
      System.err.println("Caught error:");
      e.printStackTrace();
    } finally {
      httpPost.releaseConnection();
    }
  }

  static class DatapointsHttpEntity extends AbstractHttpEntity {

    public static final String APPLICATION_JSON = "application/json";
    public static final String CONTENT_ENCODING_GZIP = "gzip";

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

      setContentType(APPLICATION_JSON);
      if (doZip) {
        setContentEncoding(CONTENT_ENCODING_GZIP);
      }
    }

    @Override
    public boolean isRepeatable() {
      return true;
    }

    @Override
    public long getContentLength() {
      return -1;
    }

    @Override
    public InputStream getContent() throws IOException, IllegalStateException {
      throw new UnsupportedOperationException(
          "Can not getContent as stream. Use writeTo(OutputStream) instead.");
    }

    @Override
    public boolean isStreaming() {
      return true;
    }

    @Override
    public void writeTo(OutputStream outputStream) throws IOException {
      if (doZip) {
        outputStream = new GZIPOutputStream(outputStream);
      }

      PrintStream ps = new PrintStream(outputStream);
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
