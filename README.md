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

To execute the simulation we can use the `gatling-maven-plugin`

```xml
 <plugin>
    <groupId>io.gatling</groupId>
    <artifactId>gatling-maven-plugin</artifactId>
    <version>${gatling-maven-plugin-version}</version>
</plugin>
```

By default, the results are stored in `${project.build.directory}/gatling`. In
the context of the Caliper-Load-Tests, all results are sent to a graphite
endpoint configured in the configuration file `test/resources/gatling.conf`. To
run the simulation simply use the `test` goal `./mvnw gatling:test`. The
following example shows a simulation class that provides the functionality of
the millionaires problem example from the
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

  val millionairesProblem = scenario("millionaires-problem-scenario") //3
    .feed(jeffFeeder) //4
    .exec(amphora.createSecret("#{secret}")) //5
    .feed(elonFeeder)
    .exec(amphora.createSecret("#{secret}"))
    .exec(ephemeral.execute(code)) //6

  setUp( //7
    millionairesProblem.inject(atOnceUsers(10) //8
    ).protocols(csProtocol)) //9
}
```

1. The class declaration, it needs to extend `Simulation`
1. The common configuration to all Carbynestack client requests, e.g. see
   [amphora-java-client](https://github.com/carbynestack/amphora/blob/master/amphora-java-client/README.md)
1. The
   [Scenario](https://gatling.io/docs/gatling/reference/current/core/scenario/)
   definition
1. A
   [Feeder](https://gatling.io/docs/gatling/reference/current/core/session/feeder/)
   is used to inject data into the virtual user
1. An amphora-java-client-request calling the `createSecret` method of the
   `io.carbynestack.amphora.client.AmphoraClient`. Using the
   [Gatling Expression Language](https://gatling.io/docs/gatling/reference/current/core/session/el/)
   we can use dynamic parameters that will be replaced with the value stored in
   the virtual user's session
1. An ephemeral-java-client-request executing the provided program, the secrets
   used by the program must be created beforehand in the same scenario, so
   ephemeral can retrieve them from the session
1. Setting up the scenario(s) we want to use in this simulation
1. Declaring that 10 virtual user will be injected at once into the
   `createSecret` scenario
1. Attaching the `cs` configuration matching the backend service configuration

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
