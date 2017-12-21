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

package ai.apptuit.metrics.dropwizard;

import com.codahale.metrics.MetricAttribute;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author Rajiv Shivane
 */
public class ApptuitReporterFactory {

  private static final DefaultStringMatchingStrategy DEFAULT_STRING_MATCHING_STRATEGY =
      new DefaultStringMatchingStrategy();

  private static final RegexStringMatchingStrategy REGEX_STRING_MATCHING_STRATEGY =
      new RegexStringMatchingStrategy();

  private TimeUnit durationUnit = TimeUnit.MILLISECONDS;

  private TimeUnit rateUnit = TimeUnit.SECONDS;

  private Set<String> excludes = Collections.emptySet();

  private Set<String> includes = Collections.emptySet();

  private boolean useRegexFilters = false;

  private EnumSet<MetricAttribute> excludesAttributes = EnumSet.noneOf(MetricAttribute.class);

  private EnumSet<MetricAttribute> includesAttributes = EnumSet.allOf(MetricAttribute.class);

  private Map<String, String> globalTags = new LinkedHashMap<>();

  private String apiKey;

  private String apiUrl;

  private ApptuitReporter.ReportingMode reportingMode;

  public void addGlobalTag(String tag, String value) {
    globalTags.put(tag, value);
  }

  public void setApiKey(String key) {
    this.apiKey = key;
  }

  public void setApiUrl(String url) {
    this.apiUrl = url;
  }

  public void setReportingMode(ApptuitReporter.ReportingMode reportingMode) {
    this.reportingMode = reportingMode;
  }

  public TimeUnit getDurationUnit() {
    return durationUnit;
  }

  public void setDurationUnit(TimeUnit durationUnit) {
    this.durationUnit = durationUnit;
  }

  public TimeUnit getRateUnit() {
    return rateUnit;
  }

  public void setRateUnit(final TimeUnit rateUnit) {
    this.rateUnit = rateUnit;
  }

  public Set<String> getIncludes() {
    return includes;
  }

  public void setIncludes(Set<String> includes) {
    this.includes = new HashSet<>(includes);
  }

  public Set<String> getExcludes() {
    return excludes;
  }

  public void setExcludes(Set<String> excludes) {
    this.excludes = new HashSet<>(excludes);
  }

  public boolean getUseRegexFilters() {
    return useRegexFilters;
  }

  public void setUseRegexFilters(boolean useRegexFilters) {
    this.useRegexFilters = useRegexFilters;
  }

  public EnumSet<MetricAttribute> getExcludesAttributes() {
    return excludesAttributes;
  }

  public void setExcludesAttributes(EnumSet<MetricAttribute> excludesAttributes) {
    this.excludesAttributes = excludesAttributes;
  }

  public EnumSet<MetricAttribute> getIncludesAttributes() {
    return includesAttributes;
  }

  public void setIncludesAttributes(EnumSet<MetricAttribute> includesAttributes) {
    this.includesAttributes = includesAttributes;
  }

  public MetricFilter getFilter() {
    final StringMatchingStrategy stringMatchingStrategy = getUseRegexFilters()
        ? REGEX_STRING_MATCHING_STRATEGY : DEFAULT_STRING_MATCHING_STRATEGY;

    return (name, metric) -> {
      // Include the metric if its name is not excluded and its name is included
      // Where, by default, with no includes setting, all names are included.
      return !stringMatchingStrategy.containsMatch(getExcludes(), name)
          && (getIncludes().isEmpty() || stringMatchingStrategy.containsMatch(getIncludes(), name));
    };
  }

  protected Set<MetricAttribute> getDisabledAttributes() {
    Set<MetricAttribute> retVal = new HashSet<>(EnumSet.allOf(MetricAttribute.class));
    retVal.removeAll(getIncludesAttributes());
    retVal.addAll(getExcludesAttributes());
    return retVal;
  }


  public ScheduledReporter build(MetricRegistry registry) {
    try {
      return new ApptuitReporter(registry, getFilter(), getRateUnit(), getDurationUnit(),
          globalTags, apiKey, apiUrl != null ? new URL(apiUrl) : null, reportingMode);
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException(e);
    }
  }

  private interface StringMatchingStrategy {

    boolean containsMatch(Set<String> matchExpressions, String metricName);
  }

  private static class DefaultStringMatchingStrategy implements StringMatchingStrategy {

    @Override
    public boolean containsMatch(Set<String> matchExpressions, String metricName) {
      return matchExpressions.contains(metricName);
    }
  }

  private static class RegexStringMatchingStrategy extends DefaultStringMatchingStrategy {
    //TODO
    /*

    private final LoadingCache<String, Pattern> patternCache;

    private RegexStringMatchingStrategy() {
        patternCache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build(new CacheLoader<String, Pattern>() {
                @Override
                public Pattern load(String regex) throws Exception {
                    return Pattern.compile(regex);
                }
            });
    }

    @Override
    public boolean containsMatch(Set<String> matchExpressions, String metricName) {
        for (String regexExpression : matchExpressions) {
            if (patternCache.getUnchecked(regexExpression).matcher(metricName).matches()) {
                // just need to match on a single value - return as soon as we do
                return true;
            }
        }

        return false;
    }
    */
  }
}
