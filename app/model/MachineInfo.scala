package model

import java.util.UUID

import play.api.libs.json._

/**
 * Created by Mikolaj Wielocha on 05/05/16
 */

case class MachineInfo(id: UUID, status: MachineStatus, averageCurrent: Double = 0.0)

object MachineInfo {

  implicit val MachineInfoWrites = new Writes[MachineInfo] {
    override def writes(mi: MachineInfo): JsValue = {
      Json.obj(
        "id" -> mi.id,
        "name" -> mi.status.name,
        "type" -> mi.status.machineType,
        "state" -> mi.status.state,
        "status" -> mi.status.status,
        "current" -> mi.status.current,
        "timestamp" -> mi.status.timestamp,
        "current_alert" -> mi.status.currentAlert,
        "average_current" -> mi.averageCurrent
      )
    }
  }
}
