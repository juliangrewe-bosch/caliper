package org.gatling.plugin.carbynestack.request.builder

import io.carbynestack.ephemeral.client.{ActivationError, ActivationResult, EphemeralMultiClient}
import io.gatling.commons.validation.{Failure, Success}
import io.gatling.core.session.Session
import io.vavr.concurrent.Future
import org.gatling.plugin.carbynestack.action.CsActionBuilder
import org.gatling.plugin.carbynestack.request.client.EphemeralClientBuilder

import java.util.UUID

class Ephemeral {

  def execute(code: String): CsActionBuilder[EphemeralMultiClient, Future[
    io.vavr.control.Either[ActivationError, java.util.List[ActivationResult]]
  ]] =
    new CsActionBuilder[EphemeralMultiClient, Future[
      io.vavr.control.Either[ActivationError, java.util.List[ActivationResult]]
    ]](
      new EphemeralClientBuilder(),
      (client: EphemeralMultiClient, session: Session) => {
        session("response").validate[java.util.List[UUID]] match {
          case Success(inputSecretIds) => client.execute(code, inputSecretIds)
          case Failure(errorMessage)   => throw new RuntimeException(errorMessage)
        }
      }
    )
}
