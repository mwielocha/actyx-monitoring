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
        "current" -> mi.status.current,
        "average_current" -> mi.averageCurrent
      )
    }
  }
}
