package model

import org.joda.time.DateTime
import play.api.libs.json._


/**
 * Created by Mikolaj Wielocha on 04/05/16
 */

case class MachineStatus(name: String, timestamp: DateTime, current: Double)

object MachineStatus {

  implicit val MachineStatusReads = Json.reads[MachineStatus]
 
}

