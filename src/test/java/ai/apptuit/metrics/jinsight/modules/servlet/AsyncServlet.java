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

package ai.apptuit.metrics.jinsight.modules.servlet;

import java.io.IOException;
import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Rajiv Shivane
 */
@WebServlet(asyncSupported = true)
public class AsyncServlet extends BaseTestServlet {

  static final String PATH = "/async";
  static final String UUID_PARAM = "uuid";

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    final String uuid = request.getParameter(UUID_PARAM);
    final AsyncContext acontext = request.startAsync();
    acontext.start(() -> {
      HttpServletResponse httpServletResponse = (HttpServletResponse) acontext.getResponse();
      try {
        System.out.println("Sleeping");
        Thread.sleep(5000);
        System.out.println("DONE Sleeping");
        httpServletResponse.getOutputStream().write(uuid.getBytes());
      } catch (InterruptedException | IOException | RuntimeException e) {
        try {
          httpServletResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.toString());
        } catch (IOException e1) {
          e1.printStackTrace();
        }
      }
      System.out.println("completing");
      acontext.complete();
    });
  }

  @Override
  public String getPath() {
    return PATH;
  }
}
