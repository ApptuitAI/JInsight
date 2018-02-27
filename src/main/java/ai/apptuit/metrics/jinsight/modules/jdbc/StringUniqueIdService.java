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

package ai.apptuit.metrics.jinsight.modules.jdbc;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Rajiv Shivane
 */
public class StringUniqueIdService {

  private static final Logger LOGGER = Logger.getLogger(StringUniqueIdService.class.getName());
  private static final int MAX_SIZE = 1000;
  private static Map<String, String> sqlIdCache = Collections.synchronizedMap(new LruMap(MAX_SIZE));


  public String getUniqueId(String sqlString) {
    if (sqlString == null) {
      return null;
    }

    String sqlId = sqlIdCache.get(sqlString);
    if (sqlId != null) {
      return sqlId;
    }

    sqlId = getIdFromServer(sqlString);
    sqlIdCache.put(sqlString, sqlId);
    return sqlId;
  }

  String getIdFromServer(String sqlString) {
    String sqlId = null;
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] hash = md.digest(sqlString.getBytes("UTF-8"));
      StringBuilder sb = new StringBuilder(2 * hash.length);
      for (byte b : hash) {
        sb.append(String.format("%02x", b & 0xff));
      }
      sqlId = sb.toString();
    } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
      LOGGER.log(Level.SEVERE, "Error generating unique id.", e);
    }
    return sqlId;
  }

  private static class LruMap extends LinkedHashMap<String, String> {

    private int maxSize;

    public LruMap(int maxSize) {
      super(maxSize + 1, 0.1F, true);
      this.maxSize = maxSize;
    }

    public boolean removeEldestEntry(Map.Entry eldest) {
      return size() > maxSize;
    }
  }
}
