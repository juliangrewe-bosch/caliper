package org.gatling.plugin.carbynestack.action

import io.carbynestack.amphora.client.AmphoraClient
import io.gatling.commons.stats.{KO, OK}
import io.gatling.core.CoreComponents
import io.gatling.core.action.Action
import io.gatling.core.session.Session
import io.gatling.core.util.NameGen

class AmphoraAction[R](
  client: AmphoraClient,
  requestFunction: (AmphoraClient, Session) => R,
  coreComponents: CoreComponents,
  val next: Action
) extends Action
    with NameGen {

  override def name: String = genName("AmphoraAction")

  override def execute(session: Session): Unit = {

    val start = coreComponents.clock.nowMillis
    var modifiedSession = session
    try {

      val response: R = requestFunction(client, modifiedSession)

      val resultList: List[R] = modifiedSession("response").asOption[List[R]].getOrElse(Nil)
      val updatedResultList = response :: resultList
      modifiedSession = modifiedSession.set("response", updatedResultList)

      coreComponents.statsEngine.logResponse(
        modifiedSession.scenario,
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
          modifiedSession.scenario,
          List("Amphora"),
          name,
          start,
          coreComponents.clock.nowMillis,
          KO,
          Some("500"),
          Some(e.getMessage),
        )
    }
    next ! modifiedSession
  }
}
