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

package ai.apptuit.metrics.jinsight.modules.whalinmemcached;

import com.whalin.MemCached.MemCachedClient;
import com.whalin.MemCached.SockIOPool;

/**
 * @author Rajiv Shivane
 */
public class BinaryWhalinMemcachedInstrumentationTest extends WhalinMemcachedInstrumentationTest {

  private static final String POOL_NAME = "BINARY_POOL";

  protected MemCachedClient getMemcachedClient() {
    String memcachedAddr = System.getProperty("memcached.addr");
    String[] servers = {memcachedAddr};
    SockIOPool pool = SockIOPool.getInstance(POOL_NAME);
    pool.setServers(servers);
    /*
    pool.setFailover( true );
    pool.setInitConn( 10 );
    pool.setMinConn( 5 );
    pool.setMaxConn( 250 );
    pool.setMaintSleep( 30 );
    pool.setNagle( false );
    pool.setSocketTO( 3000 );
    pool.setAliveCheck( true );
    */
    pool.initialize();
    return new MemCachedClient(POOL_NAME, true, true);
  }
}
