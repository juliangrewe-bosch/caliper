/*
 * Copyright (c) 2023 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/caliper.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
import io.carbynestack.amphora.client.Secret
import io.carbynestack.amphora.common.{Tag, TagValueType}
import io.gatling.core.Predef._
import io.gatling.core.session.Expression
import org.gatling.plugin.carbynestack.PreDef._

import java.math.BigInteger
import scala.jdk.CollectionConverters._
import scala.util.Random

class CarbynestackSimulation extends Simulation {

  val apolloFqdn: String = sys.env.get("APOLLO_FQDN") match {
    case Some(fqdn) if fqdn.matches("""^(\d{1,3}\.){3}\d{1,3}(?:\.sslip\.io)?$""") => fqdn
    case Some(fqdn)                                                                => throw new IllegalStateException(s"Invalid IP address format: $fqdn")
    case None                                                                      => throw new IllegalStateException("Environment variable APOLLO_FQDN not set")
  }

  val starbuckFqdn: String = sys.env.get("STARBUCK_FQDN") match {
    case Some(fqdn) if fqdn.matches("""^(\d{1,3}\.){3}\d{1,3}(?:\.sslip\.io)?$""") => fqdn
    case Some(fqdn)                                                                => throw new IllegalStateException(s"Invalid IP address format: $fqdn")
    case None                                                                      => throw new IllegalStateException("Environment variable STARBUCK_FQDN not set")
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

  val vectorAValues: Array[java.math.BigInteger] = Array.fill[java.math.BigInteger](50000)(new BigInteger("999"))
  val vectorBValues: Array[java.math.BigInteger] = Array.fill[java.math.BigInteger](50000)(new BigInteger("333"))

  val vectorASecret: Secret = Secret.of(vectorATag, vectorAValues)
  val vectorBSecret: Secret = Secret.of(vectorBTag, vectorBValues)

  val dataSize = vectorAValues.length + vectorBValues.length
  val secretValues = vectorAValues.length

  //TODO
  // pruefen, dass multiplication-triple für multiplikation genutzt werden
  // immer x Anfragen pro scenario
  // pruefen ob Knative probleme hat bei 2 Anfragen hintereinander
  // pruefen ab wann ephemeral abstürzt
  val multiplicationProgram: String =
    s"""port=regint(10000)
       |listen(port)
       |socket_id = regint()
       |acceptclientconnection(socket_id, port)
       |data = Array.create_from(sint.read_from_socket(socket_id, $dataSize))
       |scalar_product = Array(1, sint)
       |@for_range($secretValues)
       |def f(i):
       |   scalar_product[0] += data[i] * data[$secretValues + i]
       |sint.write_to_socket(socket_id, scalar_product)""".stripMargin

  val multiplicationProgramOpt: String =
    s"""port=regint(10000)
       |listen(port)
       |socket_id = regint()
       |acceptclientconnection(socket_id, port)
       |data = Array.create_from(sint.read_from_socket(socket_id, $dataSize))
       |scalar_product = Array(1, sint)
       |@for_range_opt($secretValues)
       |def f(i):
       |   scalar_product[0] += data[i] * data[$secretValues + i]
       |sint.write_to_socket(socket_id, scalar_product)""".stripMargin

  val emptyProgram: String =
    s"""port=regint(10000)
       |listen(port)
       |socket_id = regint()
       |acceptclientconnection(socket_id, port)
       |data = Array.create_from(sint.read_from_socket(socket_id, $dataSize))
       |result = Array($dataSize, sint)
       |@for_range($dataSize)
       |def f(i):
       |   result[i] = data[i]
       |sint.write_to_socket(socket_id, result)""".stripMargin

  val vectorAFeeder: Array[Map[String, Secret]] = Array(
    Map("secret" -> vectorASecret)
  )

  val vectorBFeeder: Array[Map[String, Secret]] = Array(
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

  val generateSecretsFunction: () => Secret = () => {

    val secrets = Array.fill[java.math.BigInteger](1)(
      new java.math.BigInteger((lowerBound + Random.nextLong(upperBound - lowerBound)).toString)
    )
    Secret.of(tags, secrets)
  }

  val feeder: Iterator[Map[String, Secret]] = Iterator.continually {
    Map("secret" -> generateSecretsFunction())
  }

  val uuids: Expression[java.util.List[java.util.UUID]] = session =>
    session("uuids")
      .asOption[List[java.util.UUID]]
      .getOrElse(throw new NoSuchElementException("No element of type java.util.List[java.util.UUID] found"))
      .asJava

  val ephemealScenario = scenario("ephemeral-scenario")
    .feed(vectorAFeeder)
    .exec(amphora.createSecret("#{secret}"))
    .feed(vectorBFeeder)
    .exec(amphora.createSecret("#{secret}"))
    .exec(repeat(10) {
      exec(ephemeral.execute(multiplicationProgramOpt, uuids))
    })
    .pause(60 * 5)
    .exec(repeat(10) {
      exec(ephemeral.execute(multiplicationProgram, uuids))
    })
    .pause(60 * 5)
    .exec(repeat(10) {
      exec(ephemeral.execute(multiplicationProgramOpt, uuids))
    })

  val deleteAllSecretsScenario = scenario("delete-all-secrets-scenario")
    .exec(amphora.getSecrets())
    .foreach(
      session => {
        session("uuids")
          .asOption[List[java.util.UUID]]
          .getOrElse {
            throw new NoSuchElementException(s"no UUID found in session ${session.userId}")
          }
      },
      "uuid"
    ) {
      exec(amphora.deleteSecret("#{uuid}"))
    }

  setUp(
    ephemealScenario
      .inject(atOnceUsers(1))
      .andThen(
        deleteAllSecretsScenario
          .inject(atOnceUsers(1))
      )
  ).protocols(csProtocol)
}
