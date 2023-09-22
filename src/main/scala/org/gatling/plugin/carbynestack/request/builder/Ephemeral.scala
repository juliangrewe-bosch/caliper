package org.gatling.plugin.carbynestack.request.builder

import org.gatling.plugin.carbynestack.action.EphemeralActionBuilder
import org.gatling.plugin.carbynestack.request.client.EphemeralClientBuilder

import java.util.UUID
import scala.jdk.CollectionConverters._

class Ephemeral {

  def execute(code: String, inputSecretIds: java.util.List[UUID]): EphemeralActionBuilder =
    new EphemeralActionBuilder(
      new EphemeralClientBuilder(),
      (client, _) => { client.execute(code, inputSecretIds) }
    )

  def execute(code: String): EphemeralActionBuilder =
    new EphemeralActionBuilder(
      new EphemeralClientBuilder(),
      (client, session) => {
        val responseList = session("response").asOption[List[Any]].getOrElse(Nil)
        val inputSecretIds = responseList.collect {
          case uuid: UUID => uuid
          case other =>
            throw new RuntimeException(
              s"EphemeralMultiClient.execute expected inputSecretIds of type UUID, got $other"
            )
        }.asJava
        client.execute(code, inputSecretIds)
      }
    )
}
