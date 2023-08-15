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
class CarbynestackSimulation extends Simulation {

  val apolloFqdn = sys.env
    .get("APOLLO_FQDN")
    .getOrElse(throw new IllegalStateException("Environment variable APOLLO_FQDN not set"))

  val starbuckFqdn = sys.env
    .get("STARBUCK_FQDN")
    .getOrElse(throw new IllegalStateException("Environment variable STARBUCK_FQDN not set"))

  val csProtocol = cs
    .amphoraEndpoints(
      List("http://" + "20.234.76.71.sslip.io" + "/amphora", "http://" + "20.234.79.122.sslip.io" + "/amphora")
    )
    .prime("198766463529478683931867765928436695041")
    .r("141515903391459779531506841503331516415")
    .invR("133854242216446749056083838363708373830")
    .ephemeralEndpoints(
      List("http://" + "20.234.76.71.sslip.io" + "/ephemeral", "http://" + "20.234.79.122.sslip.io" + "/ephemeral")
    )
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
    """val port = regint(10000)
       listen(port)
       val socket_id = regint()
       acceptclientconnection(socket_id, port)
       val v = sint.read_from_socket(socket_id, 2)

       val first_billionaires_net_worth = v(0)
       val second_billionaires_net_worth = v(1)
       val result = first_billionaires_net_worth < second_billionaires_net_worth

       val resp = Array(1, sint)
       resp(0) = result
       sint.write_to_socket(socket_id, resp) """

  val feeder = Iterator(jeffsNetWorth, elonsNetWorth).map { secret =>
    Map("secret" -> secret)
  }

  val jeffFeeder = Array(
    Map("secret" -> jeffsNetWorth)
  )

  val elonFeeder = Array(
    Map("secret" -> elonsNetWorth)
  )

  val millionairesProblem = scenario("millionaires-problem-scenario")
    .feed(jeffFeeder)
    .exec(amphora.createSecret("#{secret}"))
    .feed(elonFeeder)
    .exec(amphora.createSecret("#{secret}"))
    .exec(ephemeral.execute(code))

  setUp(
    millionairesProblem.inject(atOnceUsers(1)).protocols(csProtocol)
  )
}

object Main {
  def main(args: Array[String]): Unit =
    Gatling.fromMap(
      (new GatlingPropertiesBuilder)
        .simulationClass(classOf[CarbynestackSimulation].getName)
        .build,
    )
}
