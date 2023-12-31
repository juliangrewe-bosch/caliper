package simulation

import io.carbynestack.amphora.client.Secret
import io.carbynestack.amphora.common.{Tag, TagValueType}
import io.gatling.core.Predef._
import org.gatling.plugin.carbynestack.PreDef._

import scala.jdk.CollectionConverters._
import scala.util.Random

class AmphoraSimulation extends Simulation {

  //TODO add protocol (http) and endpoint /amphora or /
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

  val numberOfTags = 100
  val lengthOfTag = 100
  val numberOfSecretValuesPerSecret = 10
  val secretValueLowerBound = 1000000000L
  val secretValueUpperBound = 9999999999L

  val tags: java.util.List[Tag] =
    List
      .fill[(String, String)](numberOfTags)(
        Random.alphanumeric.take(lengthOfTag).mkString,
        Random.alphanumeric.take(lengthOfTag).mkString
      )
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

  val generateSecret: Int => Secret = (numberOfSecretValuesPerSecret: Int) => {

    val secretValues = Array.fill[java.math.BigInteger](numberOfSecretValuesPerSecret)(
      new java.math.BigInteger(
        (secretValueLowerBound + Random.nextLong(secretValueUpperBound - secretValueLowerBound)).toString
      )
    )
    Secret.of(tags, secretValues)
  }

  val generateFeeder: Int => Iterator[Map[String, Secret]] = (numberOfSecrets: Int) => {
    Iterator.continually {
      Map("secret" -> generateSecret(numberOfSecrets))
    }
  }

  def performCreateSecretsRequest(feeder: Iterator[Map[String, Secret]], numberOfSecrets: String) = {
    group(s"createSecret_${numberOfSecrets}_secret_values") {
      repeat(10) {
        feed(feeder)
          .exec(amphora.createSecret(("#{secret}")))
      }
    }
  }

  def performGetSecretsRequest(feeder: Iterator[Map[String, Secret]], numberOfSecrets: String) = {
    feed(feeder)
      .exec(amphora.createSecret("#{secret}"))
      .group(s"getSecret_${numberOfSecrets}_secret_values") {
        repeat(10) {
          exec(amphora.getSecrets())
        }
      }
      .exec(amphora.getSecrets())
      .foreach("#{uuids}", "uuid") {
        exec(amphora.deleteSecret("#{uuid"))
      }
  }

  val createSecretsScenario = scenario("amphora_createSecret_scenario")
    .exec(performCreateSecretsRequest(generateFeeder(10000), "10000"))
    .pause(30)
    .exec(performCreateSecretsRequest(generateFeeder(25000), "25000"))
    .pause(30)
    .exec(performCreateSecretsRequest(generateFeeder(50000), "50000"))
    .pause(30)
    .exec(performCreateSecretsRequest(generateFeeder(75000), "75000"))
    .pause(30)
    .exec(performCreateSecretsRequest(generateFeeder(100000), "100000"))
    .exec(amphora.getSecrets())
    .foreach("#{uuids}", "uuid"){
      exec(amphora.deleteSecret("#{uuid}"))
    }
    .pause(60)

  val getSecretsScenario = scenario("amphora_getSecrets_scenario")
    .exec(performGetSecretsRequest(generateFeeder(10000), "10000"))
    .exec(performGetSecretsRequest(generateFeeder(25000), "25000"))
    .exec(performGetSecretsRequest(generateFeeder(50000), "50000"))
    .pause(60)

  val deleteSecretScenario = scenario("amphora_deleteSecret_scenario")

  /*
  TODO
    - create high number of secrets, how does this effect subsequent actions (test for create, get ...?)
    - Implement Sort, Filter by Tags, (Pagination?)
   */

  setUp(
    createSecretsScenario
      .inject(atOnceUsers(1))
      .andThen(getSecretsScenario.inject(atOnceUsers(1)))
      .andThen(deleteSecretScenario.inject(atOnceUsers(1)))
  ).protocols(csProtocol)
}
