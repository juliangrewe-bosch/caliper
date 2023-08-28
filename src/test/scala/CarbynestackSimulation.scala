/*
 * Copyright (c) 2023 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/caliper.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
import io.carbynestack.amphora.client.Secret
import io.carbynestack.amphora.common.{Tag, TagValueType}
import io.gatling.app.Gatling
import io.gatling.core.Predef._
import io.gatling.core.config.GatlingPropertiesBuilder
import org.gatling.plugin.carbynestack.PreDef._

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

  val feeder = Iterator.continually{
    Map("secret" -> generateSecretsFunction())
  }

  val millionairesProblem = scenario("millionaires-problem-scenario")
    .feed(jeffFeeder)
    .exec(amphora.createSecret("#{secret}"))
    .feed(elonFeeder)
    .exec(amphora.createSecret("#{secret}"))
    .exec(ephemeral.execute(code))

  val createSecretSoakTest = scenario("createSecret-soak-test-30min")
    .feed(feeder)
    .exec(amphora.createSecret("#{secret}"))

  val createSecret = scenario("createSecret-scenario")
    .feed(feeder)
    .exec(amphora.createSecret("#{secret}"))

  setUp(
    createSecret.inject(atOnceUsers(1)).protocols(csProtocol)
      .andThen(millionairesProblem.inject(atOnceUsers(1)).protocols(csProtocol))
    /*.andThen(createSecretSoakTest.inject(constantUsersPerSec(550).during(60 * 30)))
    .protocols(csProtocol)*/
  )
}
