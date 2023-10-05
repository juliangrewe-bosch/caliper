package org.gatling.plugin.carbynestack.action

import io.carbynestack.amphora.client.AmphoraClient
import io.carbynestack.amphora.common.Metadata
import io.gatling.commons.stats.{KO, OK}
import io.gatling.core.CoreComponents
import io.gatling.core.action.Action
import io.gatling.core.session.Session
import io.gatling.core.util.NameGen

import scala.jdk.CollectionConverters._

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
    var stop = start
    var uuids: List[java.util.UUID] = session("uuids").asOption[List[java.util.UUID]].getOrElse(Nil)
    try {

      val response: R = requestFunction(client, session)
      stop = coreComponents.clock.nowMillis

      response match {
        case uuid: java.util.UUID => uuids = uuid :: uuids
        case uuidList: java.util.List[_] =>
          uuids = uuids ::: uuidList.asScala
            .collect {
              case metaData: Metadata => metaData
            }
            .map(metadata => metadata.getSecretId())
            .toList
        case _: Unit =>
        case other =>
          throw new IllegalArgumentException(
            s"expected argument of type java.util.UUID or List[java.util.UUID], got $other"
          )
      }
      val modifiedSession = session.set("uuids", uuids)

      coreComponents.statsEngine.logResponse(
        modifiedSession.scenario,
        List("Amphora"),
        name,
        start,
        stop,
        OK,
        None,
        None
      )
      next ! modifiedSession
    } catch {
      case e: Throwable =>
        logger.error(e.getMessage, e)
        coreComponents.statsEngine.logResponse(
          session.scenario,
          List("Amphora"),
          name,
          start,
          stop,
          KO,
          Some("500"),
          Some(e.getMessage),
        )
        next ! session.markAsFailed
    }
  }
}
