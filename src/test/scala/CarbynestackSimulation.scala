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

  val prime: String = sys.env.get("PRIME") match {
    case Some(prime) => prime
    case None        => throw new IllegalStateException("Environment variable PRIME not set")
  }

  val r: String = sys.env.get("R") match {
    case Some(r) => r
    case None    => throw new IllegalStateException("Environment variable R not set")
  }

  val invR: String = sys.env.get("INVR") match {
    case Some(invR) => invR
    case None       => throw new IllegalStateException("Environment variable INVR not set")
  }

  val program: String = sys.env.get("PROGRAM") match {
    case Some(program) => program
    case None          => throw new IllegalStateException("Environment variable PROGRAM not set")
  }

  val csProtocol = cs
    .endpoints(List(apolloFqdn, starbuckFqdn))
    .prime(prime)
    .r(r)
    .invR(invR)
    .program(program)

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

  val secretValues: Int = 10000
  val dataSize: Int = secretValues * 2

  val vectorAValues3: Array[java.math.BigInteger] =
    Array.fill[java.math.BigInteger](secretValues)(new BigInteger("999"))
  val vectorBValues3: Array[java.math.BigInteger] =
    Array.fill[java.math.BigInteger](secretValues)(new BigInteger("333"))
  val vectorAValues18: Array[java.math.BigInteger] =
    Array.fill[java.math.BigInteger](secretValues)(new BigInteger("999999999999999999"))
  val vectorBValues18: Array[java.math.BigInteger] =
    Array.fill[java.math.BigInteger](secretValues)(new BigInteger("333333333333333333"))

  val vectorASecret3: Secret = Secret.of(vectorATag, vectorAValues3)
  val vectorBSecret3: Secret = Secret.of(vectorBTag, vectorBValues3)
  val vectorASecret18: Secret = Secret.of(vectorATag, vectorAValues18)
  val vectorBSecret18: Secret = Secret.of(vectorBTag, vectorBValues18)

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

  val vectorAFeeder3: Array[Map[String, Secret]] = Array(
    Map("secret" -> vectorASecret3)
  )
  val vectorBFeeder3: Array[Map[String, Secret]] = Array(
    Map("secret" -> vectorBSecret3)
  )
  val vectorAFeeder18: Array[Map[String, Secret]] = Array(
    Map("secret" -> vectorASecret18)
  )
  val vectorBFeeder18: Array[Map[String, Secret]] = Array(
    Map("secret" -> vectorBSecret18)
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

  val ephemealScenario3 = scenario("ephemeral_scenario_3_digits")
    .feed(vectorAFeeder3)
    .group("amphora") {
      exec(amphora.createSecret("#{secret}"))
    }
    .feed(vectorBFeeder3)
    .group("amphora") {
      exec(amphora.createSecret("#{secret}"))
    }
    .group("empty_program_3_digits") {
      repeat(10) {
        exec(ephemeral.execute(emptyProgram, uuids))
      }
    }
    .pause(60 * 3)
    .group("multiplication_program_opt_3_digits") {
      repeat(10) {
        exec(ephemeral.execute(multiplicationProgramOpt, uuids))
      }
    }
    .pause(60 * 3)

  val ephemealScenario18 = scenario("ephemeral_scenario_18_digits")
    .feed(vectorAFeeder18)
    .group("amphora") {
      exec(amphora.createSecret("#{secret}"))
    }
    .feed(vectorBFeeder18)
    .group("amphora") {
      exec(amphora.createSecret("#{secret}"))
    }
    .group("empty_program_18_digits") {
      repeat(10) {
        exec(ephemeral.execute(emptyProgram, uuids))
      }
    }
    .pause(60 * 3)
    .group("multiplication_program_opt_18_digits") {
      repeat(10) {
        exec(ephemeral.execute(multiplicationProgramOpt, uuids))
      }
    }

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
    ephemealScenario3
      .inject(atOnceUsers(1))
      .andThen(
        ephemealScenario18
          .inject(atOnceUsers(1))
      )
  ).protocols(csProtocol)
}
