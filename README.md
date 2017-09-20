[![Build Status](https://img.shields.io/travis/ApptuitAI/JInsight.svg)](https://travis-ci.org/ApptuitAI/JInsight)
[![Code Coverage](https://img.shields.io/codecov/c/github/ApptuitAI/JInsight.svg)](https://codecov.io/gh/ApptuitAI/JInsight)
[![License](https://img.shields.io/github/license/ApptuitAI/JInsight.svg)](https://github.com/ApptuitAI/JInsight/blob/master/LICENSE)

# JInsight

Open source java agent to transparently instrument and monitor your Java web applications

A Java Agent to transparently collect metrics about various sub-systems
in your application.

The goal of the project is to provide out of the box monitoring for:
 * [ ] JVM metrics (Memory, GC, Threads, etc)
 * [ ] Web Server metrics (Thread Pool, Active Sessions etc)
 * [ ] Logging metrics
 * [ ] Cache metrics (EHCache, Memcache, Redis etc)
 * [ ] JDBC metrics (Connection time, PreparedStatement execution time etc)
 * [ ] HTTP metrics (Response time, HTTP status metrics)

## Design Goals

##### Transparent
Use a java agent to transparently instrument applications, without requiring
any code change

##### Low overhead
Minimal overhead on Memory and CPU footprint

##### Server agnostic
Log metrics to a file for offline processing; or report them to a server
of your choice: apptuit.ai, Prometheus, Graphite etc.


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
