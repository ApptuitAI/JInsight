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

import java.lang.reflect.Array;
import java.util.*;

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

    if (globalTags != null)
      labelCount += globalTags.size();

    if (additionalLabelNames != null)
      labelCount += additionalLabelNames.size();

    Set<String> labelNames = new LinkedHashSet<>();
    List<String> labelValues = new ArrayList<>(labelCount);

    for (String tag : tags.keySet()) {
      labelNames.add(Collector.sanitizeMetricName(tag));
    }

    labelValues.addAll(tags.values());

    if(additionalLabelNames != null) {
      labelValues.addAll(additionalLabelValues);
      labelNames.addAll(additionalLabelNames);
    }

    if(globalTags != null) {
      for (Map.Entry<String, String> tag : globalTags.entrySet()) {
        if (!labelNames.contains(Collector.sanitizeMetricName(tag.getKey()))) {
          labelNames.add(Collector.sanitizeMetricName(tag.getKey()));
          labelValues.add(tag.getValue());
        }
      }
    }


//    int startIndex = 0;
//
//    sanitizeAndAdd(labelNames, tags.keySet().toArray(new String[tags.size()]),
//            labelValues, tags.values().toArray(new String[tags.size()]), startIndex, false);
//
//    startIndex += tags.size();
//
//    if (additionalLabelNames != null) {
//      sanitizeAndAdd(labelNames, additionalLabelNames.toArray(new String[additionalLabelNames.size()]),
//              labelValues, additionalLabelValues.toArray(new String[additionalLabelNames.size()]), startIndex, false);
//
//      startIndex += additionalLabelNames.size();
//    }
//
//    //always add the global tags at the end so that they do not shadow labels in the tags and additional labels
//    if (globalTags != null) {
//      sanitizeAndAdd(labelNames, globalTags.keySet().toArray(new String[globalTags.size()]),
//              labelValues, globalTags.values().toArray(new String[globalTags.size()]), startIndex, true);
//    }

    return new Collector.MetricFamilySamples.Sample(
            Collector.sanitizeMetricName(metric),
            new ArrayList<>(labelNames),
            new ArrayList<>(labelValues),
            value);
  }

  public void sanitizeAndAdd(List<String> labelNames, String[] sourceNames,
                             List<String> labelValues, String[] sourceValues, int startIndex, boolean isGlobalTag) {
    int index = 0;
    int labelsCount = sourceNames.length;
    for (int labelIndex = index; index < labelsCount; index += 1) {
      if (!isGlobalTag || !labelNames.contains(sourceNames[index])) {
        labelNames.add(labelIndex + startIndex, Collector.sanitizeMetricName(sourceNames[index]));
        labelValues.add(labelIndex + startIndex, sourceValues[index]);
        labelIndex += 1;
      }
    }
  }

}


