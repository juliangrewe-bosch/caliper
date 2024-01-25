package simulation

import io.carbynestack.amphora.client.Secret
import io.carbynestack.amphora.common.{Tag, TagValueType}
import io.gatling.app.Gatling
import io.gatling.core.Predef._
import io.gatling.core.config.GatlingPropertiesBuilder
import org.gatling.plugin.carbynestack.PreDef._

import scala.jdk.CollectionConverters._
import scala.util.Random

class AmphoraSimulation extends Simulation {

  val apolloFqdn: String = sys.env.get("APOLLO_FQDN") match {
    case Some(fqdn) => fqdn
    case None => throw new IllegalStateException("Environment variable APOLLO_FQDN not set")
  }

  val starbuckFqdn: String = sys.env.get("STARBUCK_FQDN") match {
    case Some(fqdn) => fqdn
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

  val csProtocol = cs
    .endpoints(List(apolloFqdn, starbuckFqdn).map(fqdn => "http://" + fqdn))
    .prime(prime)
    .r(r)
    .invR(invR)

  val numberOfTags = 100
  val lengthOfTag = 100
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

  val generateFeeder: Int => Iterator[Map[String, Secret]] = (numberOfSecretValues: Int) => {
    Iterator.continually {
      Map("secret" -> generateSecret(numberOfSecretValues))
    }
  }

  def performCreateSecretRequest(feeder: Iterator[Map[String, Secret]], repeats: Int, groupLabel: String) = {
    group(groupLabel) {
      repeat(repeats) {
        feed(feeder)
          .exec(amphora.createSecret(("#{secret}")))
      }
    }
  }

  def performGetSecretsRequest(groupLabel: String) = {
    group(groupLabel) {
      repeat(1) {
        exec(amphora.getSecrets())
      }
    }
  }

  def performGetSecretRequest(groupLabel: String) = {
    exec(amphora.getSecrets())
      .group(groupLabel) {
        repeat(1) {
          foreach("#{uuids}", "uuid") {
            exec(amphora.getSecret("#{uuid}"))
          }
        }
      }
  }

//  val emptySystemScenario = scenario("empty_system_scenario")
//    .exec(performCreateSecretRequest(generateFeeder(1000),1, "createSecret_1000"))
//    .pause(60 * 3)
//    .exec(performGetSecretsRequest("getSecrets_1000"))
//    .pause(60 * 3)
//    .exec(performCreateSecretRequest(generateFeeder(10000),1, "createSecret_10000"))
//    .pause(60 * 3)
//    .exec(performGetSecretsRequest("getSecrets_11000"))
//    .pause(60 * 3)
//    .exec(performCreateSecretRequest(generateFeeder(50000), 1,"createSecret_50000"))
//    .pause(60 * 3)
//    .exec(performGetSecretsRequest("getSecrets_61000"))
//    .pause(60 * 3)
//    .exec(performCreateSecretRequest(generateFeeder(100000),1, "createSecret_100000"))
//    .pause(60 * 3)
//    .exec(performGetSecretsRequest("getSecrets_161000"))
//    .pause(60 * 3)
//    .exec(performCreateSecretRequest(generateFeeder(250000), 1,"createSecret_250000"))
//    .pause(60 * 3)
//    .exec(performGetSecretsRequest("getSecrets_411000"))
//    .pause(60 * 3)
//    .exec(performCreateSecretRequest(generateFeeder(450000), 1,"createSecret_450000"))
//    .pause(60 * 3)
//    .exec(performGetSecretsRequest("getSecrets_861000"))
//    .pause(60 * 3)

  //  val loadedSystemScenario = scenario("loaded_system_scenario")
  //    .group("createSecret_2000000_empty") {
  //      repeat(20) {
  //        feed(generateFeeder(1000))
  //          .exec(amphora.createSecret("#{secret}"))
  //      }
  //    }
  //    .pause(60 * 3)
  //    .group("createSecret_100000_loaded") {
  //      repeat(1) {
  //        feed(generateFeeder(10000))
  //          .exec(amphora.createSecret("#{secret}"))
  //      }
  //    }
  //    .pause(60 * 3)
  //    .group("getSecrets_100000_loaded") {
  //      repeat(1) {
  //        exec(amphora.getSecrets())
  //      }
  //    }
  //    .pause(60 * 3)

  //  val concurrentRequestsScenario = scenario("concurrent_requests_scenario")
  //    .group("getSecrets_400000_concurrency_10") {
  //      repeat(1) {
  //        exec(amphora.getSecrets())
  //      }
  //    }
  //    .pause(60 * 3)

//
//  val responseTimesScenario = scenario("response_times_scenario")
//    .exec(performCreateSecretRequest(generateFeeder(1000), 10, "createSecret_1000_response_times"))
//    .pause(60 * 3)
//    .exec(performCreateSecretRequest(generateFeeder(10000), 10, "createSecret_10000_response_times"))
//    .pause(60 * 3)
//    .exec(performCreateSecretRequest(generateFeeder(50000), 10, "createSecret_50000_response_times"))
//    .pause(60 * 3)
//    .exec(performCreateSecretRequest(generateFeeder(100000), 5, "createSecret_100000_response_times"))
//    .pause(60 * 3)
//    .exec(performCreateSecretRequest(generateFeeder(250000), 1, "createSecret_250000_response_times"))
//    .pause(60 * 3)
//    .exec(performCreateSecretRequest(generateFeeder(400000), 1, "createSecret_400000_response_times"))
//    .pause(60 * 3)

//  val deleteAllSecrets = scenario("deleteAllSecrets")
//    .exec(amphora.getSecrets())
//    .exec(group("deleteSecret_861000") {
//      foreach("#{uuids}", "uuid") {
//        exec(amphora.deleteSecret("#{uuid}"))
//      }
//    })

  val test = scenario("test")
    .exec(performCreateSecretRequest(generateFeeder(100), 1, "createSecret_100"))
    .pause(60 * 1)
    .exec(performGetSecretRequest("getSecret_100"))
    .pause(60 * 1)

  setUp(
    test
      .inject(atOnceUsers(1))
  ).protocols(csProtocol)
}

object Main {
  def main(args: Array[String]): Unit =
    Gatling.fromMap(
      (new GatlingPropertiesBuilder)
        .simulationClass(classOf[AmphoraSimulation].getName)
        .build,
    )
}
