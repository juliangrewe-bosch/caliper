package org.gatling.plugin.carbynestack.action

import io.carbynestack.amphora.client.AmphoraClient
import io.gatling.commons.stats.{KO, OK}
import io.gatling.core.CoreComponents
import io.gatling.core.action.Action
import io.gatling.core.session.Session
import io.gatling.core.util.NameGen

class AmphoraAction(
  client: AmphoraClient,
  requestFunction: (AmphoraClient, Session) => Unit,
  coreComponents: CoreComponents,
  val next: Action
) extends Action
    with NameGen {

  override def name: String = genName("AmphoraAction")

  override def execute(session: Session): Unit = {

    val start = coreComponents.clock.nowMillis
    try {

      requestFunction(client, session)

      coreComponents.statsEngine.logResponse(
        session.scenario,
        List("Amphora"),
        name,
        start,
        coreComponents.clock.nowMillis,
        OK,
        None,
        None
      )
    } catch {
      case e: Throwable =>
        logger.error(e.getMessage, e)
        coreComponents.statsEngine.logResponse(
          session.scenario,
          List("Amphora"),
          name,
          start,
          coreComponents.clock.nowMillis,
          KO,
          Some("500"),
          Some(e.getMessage),
        )
    }
    next ! session
  }
}
