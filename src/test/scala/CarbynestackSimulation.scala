/*
 * Copyright (c) 2023 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/caliper.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
import io.carbynestack.amphora.client.Secret
import io.carbynestack.amphora.common.{Tag, TagValueType}
import io.gatling.core.Predef._
import org.gatling.plugin.carbynestack.PreDef._

import java.math.BigInteger
import scala.jdk.CollectionConverters._
import scala.util.Random

class CarbynestackSimulation extends Simulation {

  val apolloFqdn = sys.env.get("APOLLO_FQDN") match {
    case Some(fqdn) if fqdn.matches("""^(\d{1,3}\.){3}\d{1,3}(?:\.sslip\.io)?$""") => fqdn
    case Some(fqdn) => throw new IllegalStateException(s"Invalid IP address format: $fqdn")
    case None => throw new IllegalStateException("Environment variable APOLLO_FQDN not set")
  }

  val starbuckFqdn = sys.env.get("STARBUCK_FQDN") match {
    case Some(fqdn) if fqdn.matches("""^(\d{1,3}\.){3}\d{1,3}(?:\.sslip\.io)?$""") => fqdn
    case Some(fqdn) => throw new IllegalStateException(s"Invalid IP address format: $fqdn")
    case None => throw new IllegalStateException("Environment variable STARBUCK_FQDN not set")
  }

  val csProtocol = cs
    .endpoints(List(apolloFqdn, starbuckFqdn))
    .prime("198766463529478683931867765928436695041")
    .r("141515903391459779531506841503331516415")
    .invR("133854242216446749056083838363708373830")
    .program("ephemeral-generic.default")

  val vectorATag: java.util.List[Tag] =
    List(("vector", "a"))
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

  val vectorBTag: java.util.List[Tag] =
    List(("vector", "b"))
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

  val vectorAValues: Array[java.math.BigInteger] = Array.fill[java.math.BigInteger](10000)(new BigInteger("1"))
  val vectorBValues: Array[java.math.BigInteger] = Array.fill[java.math.BigInteger](10000)(new BigInteger("2"))

  val vectorASecret = Secret.of(vectorATag, vectorAValues)
  val vectorBSecret = Secret.of(vectorBTag, vectorBValues)

  val multiplicationProgram =
    "port=regint(10000)\n" +
      "listen(port)\n" +
      "socket_id = regint()\n" +
      "acceptclientconnection(socket_id, port)\n" +
      "data = Array.create_from(sint.read_from_socket(socket_id, 20000))\n" +
      "sum = Array(1, sint)\n" +
      "@for_range(20000)\n" +
      "def f(i):\n" +
      "   data[i] = data[i]*2\n" +
      "   sum[0] += data[i]\n" +
      "sint.write_to_socket(socket_id, sum)"

  val emptyProgram =
    "port=regint(10000)\n" +
      "listen(port)\n" +
      "socket_id=regint()\n" +
      "acceptclientconnection(socket_id, port)\n" +
      "number = sint.read_from_socket(socket_id, 1)\n" +
      "resp = Array(1, sint)\n" +
      "resp[0] = number\n" +
      "sint.write_to_socket(socket_id, resp)"

  val vectorAFeeder = Array(
    Map("secret" -> vectorASecret)
  )

  val vectorBFeeder = Array(
    Map("secret" -> vectorBSecret)
  )

  val tags: java.util.List[Tag] =
    List
      .fill[(String, String)](10)(Random.alphanumeric.take(10).mkString, Random.alphanumeric.take(10).mkString)
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

  val lowerBound = 1000000000L
  val upperBound = 9999999999L

  val generateSecretsFunction = () => {

    val secrets = Array.fill[java.math.BigInteger](1)(
      new java.math.BigInteger((lowerBound + Random.nextLong(upperBound - lowerBound)).toString)
    )
    Secret.of(tags, secrets)
  }

  val feeder = Iterator.continually {
    Map("secret" -> generateSecretsFunction())
  }

  val emptyProgramScenario = scenario("empty-program-execution-scenario")
    .feed(feeder)
    .exec(amphora.createSecret("#{secret}"))
    .exec(ephemeral.execute(emptyProgram))

  val multiplicationProgramScenario = scenario("multiplication-program-scenario")
    .feed(vectorAFeeder)
    .exec(amphora.createSecret("#{secret}"))
    .feed(vectorBFeeder)
    .exec(amphora.createSecret("#{secret}"))
    .exec(ephemeral.execute(multiplicationProgram))

  val createSecretSoakTestScenario = scenario("createSecret-soak-test-30min")
    .feed(feeder)
    .exec(amphora.createSecret("#{secret}"))

  val createSecretScenario = scenario("createSecret-scenario")
    .feed(feeder)
    .exec(amphora.createSecret("#{secret}"))

  setUp(
    multiplicationProgramScenario.inject(atOnceUsers(1)).protocols(csProtocol)
    /*.andThen(createSecretSoakTest.inject(constantUsersPerSec(550).during(60 * 30)))
    .protocols(csProtocol)*/
  )
}
