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

package ai.apptuit.metrics.bci;

import ai.apptuit.metrics.dropwizard.TagEncodedMetricName;
import com.codahale.metrics.Gauge;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Status;
import net.sf.ehcache.extension.CacheExtension;
import org.jboss.byteman.rule.Rule;

/**
 * @author Rajiv Shivane
 */
public class JdbcRuleHelper extends RuleHelper {

  public static final TagEncodedMetricName ROOT_NAME = TagEncodedMetricName.decode("jdbc");
  public static final TagEncodedMetricName GET_CONNECTION_NAME = ROOT_NAME.submetric("ds.getConnection");
  public static final TagEncodedMetricName PREPARE_STATEMENT_NAME = ROOT_NAME.submetric("conn.prepareStatement");
  public static final TagEncodedMetricName EXECUTE_STATEMENT_NAME = ROOT_NAME.submetric("ps.execute");

  public JdbcRuleHelper(Rule rule) {
    super(rule);
  }
}
