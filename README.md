[![Download](https://img.shields.io/github/v/release/apptuitai/jinsight)](https://github.com/ApptuitAI/JInsight/releases/latest)
[![Build Status](https://img.shields.io/travis/ApptuitAI/JInsight.svg)](https://travis-ci.com/ApptuitAI/JInsight)
[![Sonar Cloud](https://sonarcloud.io/api/project_badges/measure?project=ai.apptuit%3Ajinsight&metric=alert_status)](https://sonarcloud.io/dashboard?id=ai.apptuit:jinsight)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/8cf7b2c6aa354b0ebb09fe61fb6d3ea1)](https://www.codacy.com/gh/ApptuitAI/JInsight)
[![Code Coverage](https://img.shields.io/codecov/c/github/ApptuitAI/JInsight.svg)](https://codecov.io/gh/ApptuitAI/JInsight)
[![License](https://img.shields.io/github/license/ApptuitAI/JInsight.svg)](https://github.com/ApptuitAI/JInsight/blob/master/LICENSE)

# JInsight

Open source java agent to transparently collect metrics about various sub-systems in your application.

JInsight currently collects metrics about the following modules/frameworks:
 * **JVM metrics**  
   [✓] Heap, [✓] GC, [✓] Threads, [✓] Classloading, [✓] Threads
 * **Web Server metrics**  
   [✓] Tomcat 8.x,  [✓] Jetty 9.X
 * **Logging metrics**  
   [✓] Log4J v1, [✓] Log4J v2, [✓] Logback
 * **Cache metrics**  
   [✓] EHCache, [✓] SpyMemcached client, [✓] Whalin memcached client,
   [✓] Redis - Jedis client, [x] Redis - Redisson
 * **JDBC metrics**  
   [✓] Generic JDBC Driver
 * **Connection pool metrics**  
   [ X ] C3PO, [ X ] HikraiCP
 * **NoSQL Databases**  
   [ X ] Cassandra, [ X ] Mongo
 * **URL connection metrics**  
   [✓] java.net.HttpURLConnection,
   [✓] Apache HTTP Client, [✓] Apache Aysnc HTTP Client, [✓] OKHttp
 * **Frameworks**  
   [ X ] Spring, [ X ] Hibernate, [ X ] Jersey, [ X ] GRPC

Refer the [Metrics Reference Guide](https://github.com/ApptuitAI/JInsight/wiki/Metrics) for a comprehensive list of metrics supported out-of-the-box

## Design Goals

##### Transparent
Use a java agent to transparently instrument applications, without requiring
any code change

##### Low overhead
Minimal overhead on Memory and CPU footprint

##### Server agnostic
Log metrics to a file for offline processing; or report them to a server
of your choice: apptuit.ai, Prometheus, Graphite etc.

## Screenshots
(click to zoom)  
[![JVM Metrics](https://raw.githubusercontent.com/ApptuitAI/JInsight/readme-attachments/screenshots/JVM_Metrics_thumb.png)](https://raw.githubusercontent.com/ApptuitAI/JInsight/readme-attachments/screenshots/JVM_Metrics.png)
[![Tomcat Metrics](https://raw.githubusercontent.com/ApptuitAI/JInsight/readme-attachments/screenshots/Tomcat_Metrics_thumb.png)](https://raw.githubusercontent.com/ApptuitAI/JInsight/readme-attachments/screenshots/Tomcat_Metrics.png)  
[![EHCache Metrics](https://raw.githubusercontent.com/ApptuitAI/JInsight/readme-attachments/screenshots/EHCache_Metrics_thumb.png)](https://raw.githubusercontent.com/ApptuitAI/JInsight/readme-attachments/screenshots/EHCache_Metrics.png)
[![Log4J Metrics](https://raw.githubusercontent.com/ApptuitAI/JInsight/readme-attachments/screenshots/Log4J_Metrics_thumb.png)](https://raw.githubusercontent.com/ApptuitAI/JInsight/readme-attachments/screenshots/Log4J_Metrics.png)

## Usage

Instrumenting a JVM with JInsight Java Agent is refreshingly simple:
1. Update your java command line to include the jinsight options:  
`java -javaagent:/usr/share/java/jinsight.jar -Djinsight.config=/etc/jinsight/jinsight-config.properties -cp helloworld.jar HelloWorld`  
2. Save API-TOKEN, global tags, frequency of reporting data etc  in the `jinsight-config.properties` file
3. (Re)start your JVM

Refer the  **[Agent installation guide](https://github.com/ApptuitAI/JInsight/wiki/UsageJInsightAgent)** for detailed instructions.

If you want to programmatically capture metrics specific to your application/workflows, beyond the [out-of-the-box metrics](https://github.com/ApptuitAI/JInsight/wiki/Metrics) provided by the JInsight agent, we recommend using [Dropwizard Metrics](http://metrics.dropwizard.io/) library for it. The **[metrics-apptuit](https://github.com/ApptuitAI/metrics-apptuit/wiki/UsageDropwizard)** library can be used to push the metrics collected by Dropwizard to  ApptuitAI.

## LICENSE

```
Copyright 2017 Agilx, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
