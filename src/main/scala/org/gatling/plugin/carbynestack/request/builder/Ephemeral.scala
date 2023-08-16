package org.gatling.plugin.carbynestack.request.builder

import io.carbynestack.ephemeral.client.{ActivationError, ActivationResult, EphemeralMultiClient}
import io.gatling.core.session.Session
import io.vavr.concurrent.Future
import org.gatling.plugin.carbynestack.action.CsActionBuilder
import org.gatling.plugin.carbynestack.request.client.EphemeralClientBuilder

import java.util.UUID
import scala.jdk.CollectionConverters._

class Ephemeral {

  def execute(code: String): CsActionBuilder[EphemeralMultiClient, Future[
    io.vavr.control.Either[ActivationError, java.util.List[ActivationResult]]
  ]] =
    new CsActionBuilder[EphemeralMultiClient, Future[
      io.vavr.control.Either[ActivationError, java.util.List[ActivationResult]]
    ]](
      new EphemeralClientBuilder(),
      (client: EphemeralMultiClient, session: Session) => {
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
