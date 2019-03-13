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

import ai.apptuit.metrics.dropwizard.TagEncodedMetricName;
import io.prometheus.client.Collector;
import io.prometheus.client.dropwizard.samplebuilder.SampleBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Extracts Metric names, label values and label names from
 * dropwizardName and additionalLabels & additionalLabelValues.
 * dropwizard has format MetricName[labelName1:labelValue1,labelName2:labelValue2.....]
 */
public class TagDecodingSampleBuilder implements SampleBuilder {
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

    Set<String> labelNames = new LinkedHashSet<>();
    List<String> labelValues = new ArrayList<>(labelCount);

    addLabels(labelNames, labelValues, tags.keySet(), tags.values(), false);
    addLabels(labelNames, labelValues, additionalLabelNames, additionalLabelValues, false);
    if (globalTags != null) {
      addLabels(labelNames, labelValues, globalTags.keySet(), globalTags.values(), true);
    }
    return new Collector.MetricFamilySamples.Sample(
            Collector.sanitizeMetricName(metric),
            new ArrayList<>(labelNames),
            labelValues,
            value);
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


