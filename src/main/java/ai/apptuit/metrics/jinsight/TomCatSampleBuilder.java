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

/**
 * Provides a sample bulder to handle the metric name, labelvalues
 * along with the labelnames in dropwizardName
 */

/**
 * Default implementation of {@link SampleBuilder}.
 * Sanitises the metric name if necessary.
 *
 * @see io.prometheus.client.Collector#sanitizeMetricName(String)
 */
public class TomCatSampleBuilder implements SampleBuilder {
  @Override
  public Collector.MetricFamilySamples.Sample createSample(final String dropwizardName,
                                                           final String nameSuffix,
                                                           final List<String> additionalLabelNames,
                                                           final List<String> additionalLabelValues,
                                                           final double value) {

    final String suffix = nameSuffix == null ? "" : nameSuffix;
    TagEncodedMetricName mn = TagEncodedMetricName.decode(dropwizardName);
    final String metric = mn.getMetricName() + suffix;
    List<String> labelNames = new ArrayList<String>();
    List<String> labelValues = new ArrayList<String>();
    labelNames.addAll(mn.getTags().keySet());
    labelNames.addAll(additionalLabelNames);
    labelValues.addAll(mn.getTags().values());
    labelValues.addAll(additionalLabelValues);


    //
    //        String metricName;

    //        String[] strings = dropwizardName.split("[\\[\\]]");
    //        final String suffix = nameSuffix == null ? "" : nameSuffix;
    //        metricName = strings[0] + suffix;
    //        if(strings.length > 1)
    //        {
    //            String[] labelAndValues = strings[1].split("[,]");
    //            for(String labelAndValue : labelAndValues)
    //            {
    //                try{
    //                    String[] temp = labelAndValue.split("[:]");
    //                    labelNames.add(temp[0]);
    //                    labelValues.add(temp[1]);
    //                }
    //                catch (ArrayIndexOutOfBoundsException aie)
    //                {
    //                    System.out.println(" Labels and label values are not equal");
    //                    aie.printStackTrace();
    //                }
    //            }
    //        }
    return new Collector.MetricFamilySamples.Sample(
            Collector.sanitizeMetricName(metric),
            new ArrayList<String>(labelNames),
            new ArrayList<String>(labelValues),
            value);
  }
}

