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

package ai.apptuit.metrics.jinsight;

import ai.apptuit.metrics.client.TagEncodedMetricName;
import io.prometheus.client.Collector;
import io.prometheus.client.dropwizard.samplebuilder.SampleBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Extracts Metric names, label values and label names from dropwizardName, additionalLabels and additionalLabelValues.
 * dropwizard has format MetricName[labelName1:labelValue1,labelName2:labelValue2.....]
 */
public class TagDecodingSampleBuilder implements SampleBuilder {

    private final PrometheusTimeSeriesNameCache cache = new PrometheusTimeSeriesNameCache(250000);
    private final Map<String, String> globalTags;

    public TagDecodingSampleBuilder(Map<String, String> globalTags) {
        this.globalTags = globalTags;
    }

    @Override
    public Collector.MetricFamilySamples.Sample createSample(final String dropwizardName,
            final String nameSuffix,
            final List<String> additionalLabelNames,
            final List<String> additionalLabelValues,
            final double value) {
        PrometheusTimeSeriesName timeSeries;
        timeSeries = cache.getPrometheusTimeSeriesName(dropwizardName, nameSuffix, additionalLabelNames,
                additionalLabelValues);

        return new Collector.MetricFamilySamples.Sample(
                timeSeries.metricName,
                timeSeries.labelNames,
                timeSeries.labelValues,
                value);
    }

    private class PrometheusTimeSeriesNameCache {

        private Map<String, PrometheusTimeSeriesName> cachedTSNs = null;

        public PrometheusTimeSeriesNameCache(int capacity) {
            if (capacity > 0) {
                cachedTSNs = Collections.synchronizedMap(
                        new LRUCache<>(capacity));
            }
        }

        private PrometheusTimeSeriesName getPrometheusTimeSeriesName(String dropwizardName, String nameSuffix,
                List<String> additionalLabelNames, List<String> additionalLabelValues) {
            PrometheusTimeSeriesName timeSeries;
            if (cachedTSNs != null && (additionalLabelNames == null || additionalLabelNames.size() <= 1)) {
                String cacheKey = dropwizardName;
                if (nameSuffix != null) {
                    cacheKey += "\n" + nameSuffix;
                }
                if (additionalLabelNames != null && additionalLabelNames.size() == 1) {
                    cacheKey += "\n" + additionalLabelNames.get(0);
                    cacheKey += "\n" + additionalLabelValues.get(0);
                }
                timeSeries = cachedTSNs.get(cacheKey);
                if (timeSeries == null) {
                    timeSeries = new PrometheusTimeSeriesName(dropwizardName, nameSuffix, additionalLabelNames,
                            additionalLabelValues);
                    cachedTSNs.put(cacheKey, timeSeries);
                }
            } else {
                timeSeries = new PrometheusTimeSeriesName(dropwizardName, nameSuffix, additionalLabelNames,
                        additionalLabelValues);
            }
            return timeSeries;
        }

    }

    private static class LRUCache<K, V> extends LinkedHashMap<K, V> {

        private final int capacity;

        public LRUCache(int capacity) {
            super(capacity + 1, 1.0f, true);
            this.capacity = capacity;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return (size() > this.capacity);
        }

    }

    private class PrometheusTimeSeriesName {

        private final String metricName;
        private final List<String> labelNames;
        private final List<String> labelValues;

        public PrometheusTimeSeriesName(final String dropwizardName,
                final String nameSuffix,
                final List<String> additionalLabelNames,
                final List<String> additionalLabelValues) {

            final String suffix = nameSuffix == null ? "" : nameSuffix;
            TagEncodedMetricName mn = TagEncodedMetricName.decode(dropwizardName);
            final String metric = mn.getMetricName() + suffix;
            Map<String, String> tags = mn.getTags();

            int labelCount = tags.size();
            if (globalTags != null) {
                labelCount += globalTags.size();
            }
            if (additionalLabelNames != null) {
                labelCount += additionalLabelNames.size();
            }

            Set<String> labelNames = new LinkedHashSet<>(labelCount);
            List<String> labelValues = new ArrayList<>(labelCount);

            addLabels(labelNames, labelValues, tags.keySet(), tags.values(), false);
            addLabels(labelNames, labelValues, additionalLabelNames, additionalLabelValues, false);
            if (globalTags != null) {
                addLabels(labelNames, labelValues, globalTags.keySet(), globalTags.values(), true);
            }

            this.metricName = Collector.sanitizeMetricName(metric);
            this.labelNames = new ArrayList<>(labelNames);
            this.labelValues = labelValues;
        }

        private void addLabels(Set<String> labelNames, List<String> labelValues, Collection<String> sourceNames,
                Collection<String> sourceValues, boolean skipDuplicate) {
            if (sourceNames == null) {
                return;
            }

            Iterator<String> values = sourceValues.iterator();
            for (String tag : sourceNames) {
                String value = values.next();
                String sanitizedName = Collector.sanitizeMetricName(tag);
                if (skipDuplicate && labelNames.contains(sanitizedName)) {
                    continue;
                }
                labelNames.add(sanitizedName);
                labelValues.add(value);
            }
        }
    }
}


