package api

import scala.concurrent.Future
import java.util.UUID

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import play.api.libs.ws._

import model._
import org.joda.time.DateTime

import play.api.libs.json._


/**
 * Created by Mikolaj Wielocha on 04/05/16
 */

@Singleton
class Client @Inject() (private val ws: WSClient)(implicit private val ec: ExecutionContext) {

  val UUIDRegex = "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}".r

  val machineUrl = "http://machinepark.actyx.io/api/v1/machine"

  def getMachineStatus(machineId: UUID): Future[MachineData] = {
    ws.url(s"$machineUrl/$machineId").get().map {
      case response => (response.json.as[JsObject]
          ++ Json.obj("id" -> machineId)).as[MachineData]
    }
  }

  val machinesUrl = "http://machinepark.actyx.io/api/v1/machines"

  def getMachines: Future[List[UUID]] = {
    ws.url(machinesUrl).get().map {
      response => response.json.as[List[String]].flatMap {
        url => UUIDRegex.findFirstIn(url)
      }.map(UUID.fromString)
    }
  }
}

