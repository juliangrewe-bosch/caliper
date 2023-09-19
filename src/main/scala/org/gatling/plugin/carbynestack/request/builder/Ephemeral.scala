package org.gatling.plugin.carbynestack.request.builder

import org.gatling.plugin.carbynestack.action.EphemeralActionBuilder
import org.gatling.plugin.carbynestack.request.client.EphemeralClientBuilder

import java.util.UUID

class Ephemeral {

  def execute(code: String, inputSecretIds: java.util.List[UUID]): EphemeralActionBuilder =
    new EphemeralActionBuilder(
      new EphemeralClientBuilder(),
      client => { client.execute(code, inputSecretIds) }
    )
}
