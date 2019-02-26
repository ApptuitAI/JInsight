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
import java.util.List;
import java.util.Map;

/**
 * Extracts Metric names, label values and label names from
 * dropwizardName and additionalLabels & additionalLabelValues.
 * dropwizard has format MetricName[labelName1:labelValue1,labelName2:labelValue2.....]
 */
public class TagDecodingSampleBuilder implements SampleBuilder {
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
    if(additionalLabelNames!=null)
      labelCount+=additionalLabelNames.size();

    List<String> labelNames = new ArrayList<>(labelCount);
    for (String tag : tags.keySet()) {
      labelNames.add(Collector.sanitizeMetricName(tag));
    }
    labelNames.addAll(additionalLabelNames);

    List<String> labelValues = new ArrayList<>(labelCount);
    labelValues.addAll(tags.values());
    labelValues.addAll(additionalLabelValues);

    return new Collector.MetricFamilySamples.Sample(
            Collector.sanitizeMetricName(metric),
            labelNames,
            labelValues,
            value);
  }
}

