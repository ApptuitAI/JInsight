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
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
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
  private Map<String, String> globalTags;
  private String token;
  private CloseableHttpClient httpclient;

  public ApptuitPutClient(String token, Map<String, String> globalTags) {
    this.globalTags = globalTags;
    this.token = token;
    this.httpclient = HttpClients.createDefault();
  }

  public void put(Collection<DataPoint> dataPoints) {

    DatapointsHttpEntity entity = new DatapointsHttpEntity(dataPoints);

    HttpPost httpPost = new HttpPost(PUT_API_URI);
    httpPost.setEntity(entity);
    httpPost.setHeader("Authorization", "Bearer " + token);

    httpPost.setConfig(RequestConfig.copy(
        RequestConfig.DEFAULT)
        .setConnectTimeout(CONNECT_TIMEOUT_MS)
        .setSocketTimeout(SOCKET_TIMEOUT_MS)
        .setConnectionRequestTimeout(CONNECTION_REQUEST_TIMEOUT_MS)
        .build()
    );

    ResponseHandler<String> responseHandler = response -> {
      HttpEntity respEntity = response.getEntity();
      int status = response.getStatusLine().getStatusCode();
      if (status >= 200 && status < 300) {
        return respEntity != null ? EntityUtils.toString(respEntity) : null;
      } else {
        System.out.println(respEntity != null ? EntityUtils.toString(respEntity) : null);
        throw new ClientProtocolException("Unexpected response status: " + status);
      }
    };

    try {
      String responseBody = httpclient.execute(httpPost, responseHandler);
      System.out.println("----------------------------------------");
      System.out.println(responseBody);
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      httpPost.releaseConnection();
    }
  }

  private class DatapointsHttpEntity extends AbstractHttpEntity {

    private final Collection<DataPoint> dataPoints;

    public DatapointsHttpEntity(Collection<DataPoint> dataPoints) {
      this.dataPoints = dataPoints;
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
    }
  }
}
