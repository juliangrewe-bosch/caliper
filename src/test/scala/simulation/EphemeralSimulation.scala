package simulation

import io.carbynestack.amphora.client.Secret
import io.carbynestack.amphora.common.{Tag, TagValueType}
import io.gatling.core.Predef._
import org.gatling.plugin.carbynestack.PreDef._

import scala.jdk.CollectionConverters._

class EphemeralSimulation extends Simulation {

  val apolloFqdn: String = sys.env.get("APOLLO_FQDN") match {
    case Some(fqdn) if fqdn.matches("""^(\d{1,3}\.){3}\d{1,3}(?:\.sslip\.io)?$""") => fqdn
    case Some(fqdn) => throw new IllegalStateException(s"Invalid IP address format: $fqdn")
    case None => throw new IllegalStateException("Environment variable APOLLO_FQDN not set")
  }

  val starbuckFqdn: String = sys.env.get("STARBUCK_FQDN") match {
    case Some(fqdn) if fqdn.matches("""^(\d{1,3}\.){3}\d{1,3}(?:\.sslip\.io)?$""") => fqdn
    case Some(fqdn) => throw new IllegalStateException(s"Invalid IP address format: $fqdn")
    case None => throw new IllegalStateException("Environment variable STARBUCK_FQDN not set")
  }

  val prime: String = sys.env.get("PRIME") match {
    case Some(prime) => prime
    case None => throw new IllegalStateException("Environment variable PRIME not set")
  }

  val r: String = sys.env.get("R") match {
    case Some(r) => r
    case None => throw new IllegalStateException("Environment variable R not set")
  }

  val invR: String = sys.env.get("INVR") match {
    case Some(invR) => invR
    case None => throw new IllegalStateException("Environment variable INVR not set")
  }

  val program: String = sys.env.get("PROGRAM") match {
    case Some(program) => program
    case None => throw new IllegalStateException("Environment variable PROGRAM not set")
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

  val secretValues: Int = 10
  val dataSize: Int = secretValues * 2

  val feeder = Iterator.continually {
    Map(
      "secret" -> Secret
        .of(vectorBTag, Array.fill[java.math.BigInteger](secretValues)(new java.math.BigInteger("10")))
    )
  }

  def performDeleteSecretRequest() = {
    exec(amphora.getSecrets())
      .foreach("#{uuids}", "uuid") {
        exec(amphora.deleteSecret("#{uuid}"))
      }
  }

  val scalarValueProgram: String =
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

  val scalarValueProgramOpt: String =
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

  val additionProgram: String =
    s"""port=regint(10000)
       |listen(port)
       |socket_id = regint()
       |acceptclientconnection(socket_id, port)
       |data = Array.create_from(sint.read_from_socket(socket_id, $dataSize))
       |result = Array(1, sint)
       |@for_range_opt($secretValues)
       |def f(i):
       |   result[0] += data[i] + data[$secretValues + i]
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

  val emptyProgramScenario = scenario("emptyProgram")
    .feed(feeder)
    .exec(amphora.createSecret("#{secret}"))
    .feed((feeder))
    .exec(amphora.createSecret("#{secret}"))
    .exec(amphora.getSecrets())
    .group("secret_values_10000") {
      repeat(1) {
        exec(ephemeral.execute(emptyProgram, "#{uuids}"))
      }
    }
    .pause(60 * 3)

  val scalarValueOptProgramScenario = scenario("scalarValueOptProgrma")
    .feed(feeder)
    .exec(amphora.createSecret("#{secret}"))
    .feed((feeder))
    .exec(amphora.createSecret("#{secret}"))
    .exec(amphora.getSecrets())
    .group("secret_values_10000") {
      repeat(1) {
        exec(ephemeral.execute(scalarValueProgramOpt, "#{uuids}"))
      }
    }
    .pause(60 * 3)

  val deleteAllSecretsAfterEmptyProgramScenario = scenario("deleteAllSecretsAfterEmptyProgram")
    .exec(performDeleteSecretRequest())

  val deleteAllSecretsAfterScalarValueOptProgramScenario = scenario("deleteAllSecretsAfterScalarValueOptProgram")
    .exec(performDeleteSecretRequest())

  setUp(
    emptyProgramScenario
      .inject(atOnceUsers(1))
      .andThen(deleteAllSecretsAfterEmptyProgramScenario.inject(atOnceUsers(1)))
      .andThen(scalarValueOptProgramScenario.inject(atOnceUsers(1)))
      .andThen(deleteAllSecretsAfterScalarValueOptProgramScenario.inject(atOnceUsers(1)))
  ).protocols(csProtocol)
}