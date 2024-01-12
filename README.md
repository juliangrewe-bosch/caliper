# Carbyne Stack Caliper Load Testing Harness

[![Codacy Badge](https://app.codacy.com/project/badge/Grade/233198c332f3486ea69057fb9938917e)](https://app.codacy.com/gh/carbynestack/caliper/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)
[![Known Vulnerabilities](https://snyk.io/test/github/carbynestack/caliper/badge.svg)](https://snyk.io/test/github/carbynestack/caliper)
[![pre-commit](https://img.shields.io/badge/pre--commit-enabled-brightgreen?logo=pre-commit&logoColor=white)](https://github.com/pre-commit/pre-commit)
[![Contributor Covenant](https://img.shields.io/badge/Contributor%20Covenant-2.1-4baaaa.svg)](CODE_OF_CONDUCT.md)

> **DISCLAIMER**: Carbyne Stack Caliper is in *proof-of-concept* stage. The
> software is not ready for production use. It has neither been developed nor
> tested for a specific use case.

Caliper is the Load-Testing-as-Code harness for
[Carbyne Stack](https://github.com/carbynestack). This project is based on the
[Gatling](https://github.com/gatling/gatling) load test tool and provides a
plugin that can be used to communicate with backend services of a Carbyne Stack
Virtual Cloud using the dedicated java-clients.

## Components

### Protocol

The `cs` object is used to provide a common configuration that is shared between
all virtual users. A list of Service endpoint URIs and the SPDZ parameters
matching the backend service configuration are used to initialize a client.

### Action

To test the performance of one or multiple backend services of a Carbyne Stack
Virtual Cloud we create scenarios that make requests to a backend service. The
`exec` method is used to execute an Action, in the context of this plugin,
actions are requests performed by a client that will be sent during a
simulation.

## Usage

To execute a simulation we can use the `gatling-maven-plugin`. Currently two
Carbyne Stack Clients are supported (Amphora and Ephemeral), so for each service
we create a collection of tests in a simulation class located under
`test/scala/`. You can control which simulations will be triggered with the
`includes` filter.

```xml

<plugin>
    <groupId>io.gatling</groupId>
    <artifactId>gatling-maven-plugin</artifactId>
    <version>${maven-gatling-plugin.version}</version>
    <configuration>
        <runMultipleSimulations>true</runMultipleSimulations>
        <includes>
            <include>simulation.*</include>
        </includes>
    </configuration>
</plugin>
```

By default, the results are stored in `${project.build.directory}/gatling`.
Caliper uses [prometheus](https://prometheus.io/) to visualize the results,
therefore all results are sent to a graphite endpoint configured in the
configuration file`test/resources/gatling.conf`. To run Gatling tests simply use
the `test` goal `./mvnw gatling:test`. The following example shows a simulation
class that provides the functionality of the millionaires problem example from
the
[Carbyne Stack Tutorial](https://carbynestack.io/getting-started/millionaires/).

```scala
class CarbynestackSimulation extends Simulation { //1

  val csProtocol = cs //2
    .endpoints(List(apolloFqdn, starbuckFqdn))
    .prime("198766463529478683931867765928436695041")
    .r("141515903391459779531506841503331516415")
    .invR("133854242216446749056083838363708373830")
    .program("ephemeral-generic.default")

  val jeffTag: java.util.List[Tag] =
    List(("billionaire", "jeff"))
      .map(
        x =>
          Tag
            .builder()
            .key(x._1)
            .value(x._2)
            .valueType(TagValueType.STRING)
            .build()
      )
      .asJava

  val elonTag: java.util.List[Tag] =
    List(("billionaire", "elon"))
      .map(
        x =>
          Tag
            .builder()
            .key(x._1)
            .value(x._2)
            .valueType(TagValueType.STRING)
            .build()
      )
      .asJava

  val jeffSecret: Array[java.math.BigInteger] = Array(new java.math.BigInteger("180"))
  val elonSecret: Array[java.math.BigInteger] = Array(new java.math.BigInteger("177"))

  val jeffsNetWorth = Secret.of(jeffTag, jeffSecret)
  val elonsNetWorth = Secret.of(elonTag, elonSecret)

  val code =
    "port=regint(10000)\n" +
      "listen(port)\n" +
      "socket_id = regint()\n" +
      "acceptclientconnection(socket_id, port)\n" +
      "v = sint.read_from_socket(socket_id, 2)\n" +
      "first_billionaires_net_worth = v[0]\n" +
      "second_billionaires_net_worth= v[1]\n" +
      "result = first_billionaires_net_worth < second_billionaires_net_worth\n" +
      "resp = Array(1, sint)\n" +
      "resp[0] = result\n" +
      "sint.write_to_socket(socket_id, resp)"

  val jeffFeeder = Array(
    Map("secret" -> jeffsNetWorth)
  )

  val elonFeeder = Array(
    Map("secret" -> elonsNetWorth)
  )

  val uuids: Expression[java.util.List[java.util.UUID]] = session =>
    session("uuids")
      .asOption[List[java.util.UUID]]
      .asJava

  val millionairesProblem = scenario("millionaires-problem-scenario") //3
    .feed(jeffFeeder) //4
    .exec(amphora.createSecret("#{secret}")) //5
    .feed(elonFeeder)
    .exec(amphora.createSecret("#{secret}"))
    .exec(ephemeral.execute(code, uuids)) //6 TODO UPDATE uuids is Expression now

  setUp( //7
    millionairesProblem
      .inject(
        atOnceUsers(1) //8
      )
      .protocols(csProtocol) //9
  )
}

```

1. The class declaration, it needs to extend `Simulation`.
1. The common configuration to all Carbyne Stack clients, see
   [amphora-java-client](https://github.com/carbynestack/amphora/blob/master/amphora-java-client/README.md)
1. The
   [Scenario](https://gatling.io/docs/gatling/reference/current/core/scenario/)
   definition.
1. A
   [Feeder](https://gatling.io/docs/gatling/reference/current/core/session/feeder/)
   is used to inject data into the virtual user.
1. An amphora-java-client-request calling the `createSecret` method of the
   `io.carbynestack.amphora.client.AmphoraClient`. Using the
   [Gatling Expression Language](https://gatling.io/docs/gatling/reference/current/core/session/el/)
   we can use dynamic parameters that will be replaced with the value stored in
   the virtual user's session.
1. An ephemeral-java-client-request executing the provided program, the secrets
   used by the program must be created beforehand and are used as input to the
   function.
1. Setting up the scenario(s) we want to use in this simulation.
1. Declaring that 1 virtual user will be injected into the `millionairesProblem`
   scenario.
1. Attaching the `cs` configuration matching the backend service configuration.

## Test Infrastructure

To run the load-tests a `Carbyne Stack Virtual Cloud` has to be deployed. The
\[LINK\] IaC repository is used to deploy a `two-party VC` hosted on Microsoft
Azure. The following resources are created by running the IaC deployment:

- *PrivateAksStack*
  - Deploys an AzureVM that is later peered with the Carbyne Stack VC
- *PrivateAksVirtualCloudStack*
  - Deploys two private AKS cluster and a two-party Carbyne Stack VC
- *GraphiteExporter*
  - Deploys an app to transform and expose metrics for Prometheus
- *Prometheus*
  - Deploys a Prometheus server and config resources

The following resources have to be created beforehand:

| Resource          | Name                             | Role                                                  | Usage                           | Expiration |
| ----------------- | -------------------------------- | ----------------------------------------------------- | ------------------------------- | ---------- |
| Managed Identity  | `caliper-aks-managed-identity`   | `Private DNS Zone Contributor`, `Network Contributor` | Peer networks ?                 |            |
| Service Principal | `caliper-test-infrastructure-sp` | `Contributer-Role`                                    | Authenticate Terraform to Azure | 2/27/2024  |

## Report

The report provides metrics about resource consumption and response times of the
deployed services:

### cAdvisor

[cAdvisor](https://github.com/google/cadvisor) is a running daemon that
collects, aggregates, processes, and exports information about running
containers. The following metrics are currently added to the report:

| Metric Name                              | Type    | Description                           | Unit    | Option parameter |
| ---------------------------------------- | ------- | ------------------------------------- | ------- | ---------------- |
| `container_memory_working_set_bytes`     | Gauge   | Current working set                   | bytes   | memory           |
| `container_cpu_usage_seconds_total`      | Counter | Cumulative cpu time consumed          | seconds | cpu              |
| `container_fs_writes_bytes_total`        | Counter | Cumulative count of bytes written     | bytes   | diskIO           |
| `container_fs_reads_bytes_total`         | Counter | Cumulative count of bytes read        | bytes   | diskIO           |
| `container_network_receive_bytes_total`  | Counter | Cumulative count of bytes received    | bytes   | network          |
| `container_network_transmit_bytes_total` | Counter | Cumulative count of bytes transmitted | bytes   | network          |

### Gatling

Gatling can export
[metrics](https://gatling.io/docs/gatling/guides/realtime_monitoring) over the
`Graphite plaintext` protocol. For this, `graphite` must be added to the data
writers and a target host, which in this context is the
[GraphiteExporter](https://github.com/prometheus/graphite_exporter), has to be
specified:

```text
 graphite {
      #light = false
      host = "10.1.1.5"           # AKS node IP
      port = 32766                # GraphiteExporter
      protocol = "tcp"
      rootPathPrefix = "gatling"
      bufferSize = 8192
      writePeriod = 60
    }
```

The following metrics are then send to prometheus:

`caliper{simulation, group, metric, scope}`

caliper{simulation="amphorasimulation", group="secret_values_10000",
metric="percentiles99", scope="ok"}

### GitHub Actions Workflow

The GitHub Actions Workflow can be split into two parts:

`Deploy an AzureVM` this step deploys an AzureVM. The VM is later peered with
the AKS-Clusters that form the `Carbyne Stack VC`.

- The Caliper-load-tests are executed from the VM.

`Run load-tests` this step connects to the AzureVM via SSH and executes
`scripts/run_caliper_load_tests.sh`. The VM then deploys the aks cluster and all
resouces for a working `VC`. After deploying all relevant resources, the
Caliper-Load-Tests are executed using the Gatling Simulation classes
`src/test/scala/simulation/`. The final step is to collect all metrics for the
load-test report and create the actual report using
`scripts/generate_report.py`.

The following secrets are mandatory to run the GitHub Actions Workflow:

| Secret                  | Description                                                              |
| ----------------------- | ------------------------------------------------------------------------ |
| `AZURE_CREDENTIALS`     | Azure CLI Github Action                                                  |
| `AZURE_SUBSCRIPTION_ID` | Authenticate Terraform to Azure                                          |
| `AZURE_CLIENT_SECRET`   | Authenticate Terraform to Azure                                          |
| `AZURE_TENANT_ID`       | Authenticate Terraform to Azure                                          |
| `AZURE_CLIENT_ID`       | Authenticate Terraform to Azure                                          |
| `CALIPER_PAT`           | Caliper maven project uses this to download Clients from Github Packages |
| `ADMIN_PASSWORD`        | Password for the AzureVM that is peered with the AKS                     |

### Add/ Remove Test-cases

To add or remove test-cases the following steps must be peformed:

A Simulation class contains multiple scenario(s)

- scenario(s) definieren

  - was sind scenarios (deckt mehrere Testfälle ab)

- groups innerhalb eines scenarios erstellen

  - Was sind groups (deckt mindestesns einen Testfall ab)

- PromQL queries collecting metrics for the tests needs to be added to
  `scripts/generate_report.py`

- Graphen sind unter /img/simulationX/scenarioY/groupZ/ gespeichert

  - Datei(en) erstellen und Grafiken einbinden
  - Datei(en) nav.yaml hinzufügen

## Namesake

> A *caliper* \[...\] is a device used to measure the dimensions of an object
> ([Source](https://en.wikipedia.org/wiki/Calipers)).

The object being measured in our case is Carbyne Stack. The dimensions are
primarily performance and scalability.

## License

The Carbyne Stack *Caliper Load-Testing-as-Code harness* is open-sourced under
the Apache License 2.0. See the [LICENSE](LICENSE) file for details.

### 3rd Party Licenses

For information on how license obligations for 3rd party OSS dependencies are
fulfilled see the [README](https://github.com/carbynestack/carbynestack) file of
the Carbyne Stack repository.

## Contributing

Please see the Carbyne Stack
[Contributor's Guide](https://github.com/carbynestack/carbynestack/blob/master/CONTRIBUTING.md)
