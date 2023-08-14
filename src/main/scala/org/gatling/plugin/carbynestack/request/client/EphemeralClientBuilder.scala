package org.gatling.plugin.carbynestack.request.client
import io.carbynestack.ephemeral.client.{EphemeralEndpoint, EphemeralMultiClient}
import org.gatling.plugin.carbynestack.protocol.CsComponents

import java.util

class EphemeralClientBuilder extends ClientBuilder[EphemeralMultiClient] {

  override def build(csComponents: CsComponents): EphemeralMultiClient = {

    new EphemeralMultiClient.Builder()
      .withEndpoints(csComponents.protocol.endpoints)
      .build()
  }

}
