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

package ai.apptuit.metrics.jinsight.modules.ehcache;

import ai.apptuit.metrics.dropwizard.TagEncodedMetricName;
import ai.apptuit.metrics.jinsight.RegistryService;
import ai.apptuit.metrics.jinsight.modules.common.RuleHelper;
import com.codahale.metrics.MetricRegistry;
import net.sf.ehcache.Cache;
import org.jboss.byteman.rule.Rule;

/**
 * @author Rajiv Shivane
 */
public class EhcacheRuleHelper extends RuleHelper {

  public static final TagEncodedMetricName ROOT_NAME = TagEncodedMetricName.decode("ehcache");

  public EhcacheRuleHelper(Rule rule) {
    super(rule);
  }

  public void monitor(Cache cache) {
    MetricRegistry registry = RegistryService.getMetricRegistry();
    cache.registerCacheExtension(new CacheLifecycleListener(cache, registry));

  }

}
