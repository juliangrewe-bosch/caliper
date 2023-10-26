# Caliper Load-Testing Harness

## Test Scenarios

### ScenarioX

- Amphora 10.000 Secret Values
- Ephemeral:

```scala
ephemeralProgram
```

### ScenarioY

- Amphora 100.000 Secret-Values

## Report

Caliper generates a report that is uploaded to github-pages. The report is split
up into a Gatling and a cAdvisor section.

The metrics are collected via prometheus, Prometheus-Operator(LINK), two jobs
knative exposes cAdvisor metrics in the prometheus time series format gatling
metrics are exposed via the prometheus-exporter(LINK). The prometheus time
series are transformed into plots using pyplot(PROMETHEUS-LIBRARY-LINK).

Former reports are provided with the version-plugin(LINK) in the format x-y-z.

### Gatling

This section provides latency metrics such as a respone time distribution and
standard statistics. The response time is plotted for each request and
statistics such as min, max, average, standard deviation and percentiles are
calculated per group.

Gatling only supports exporting metrics in graphite style, so graphite exporter
is used timeseries database only support numerical values so for a single
request all statistics equal the response time for readability all different
metric types are stored and calculated afterwards (LINK TO GATLING GRAPHITE)
Groups are defined in the gatling carbynestacksimulation and consist of one or
multiple requests that are executed sequentially. EXAMPLES

The report also includes failed requests and calculates the metrics for failed
requests seperately.

### cAdvisor

[cAdvisor](https://github.com/google/cadvisor) exports information about running
containers and exposes them as Prometheus metrics.

container_cpu_usage_s container_memory_working_set_bytes:

container_network\_(receive/transmit)\_bytes_total: Cumulative count of bytes
received/transmitted

container_fs\_(reads/writes)\_bytes_total: Cumulative count of bytes
read/written b: c:
