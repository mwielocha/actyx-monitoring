package model

import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Created by Mikolaj Wielocha on 04/05/16
 */

/*
{
 "name": "DMG DMU 40eVo [#50]",
 "timestamp": "2016-05-08T15:39:59.065450",
 "current": 12.16,
 "state": "working",
 "location": "0.0,0.0",
 "current_alert": 14,
 "type": "mill"
}
*/

case class MachineStatus(
  name: String,
  timestamp: DateTime,
  current: Double,
  state: String,
  location: String,
  machineType: String,
  currentAlert: Double) {

  val status = if(current > currentAlert) "critical"
    else if(current == currentAlert) "warning"
    else "normal"
}

object MachineStatus {

  implicit val MachineStatusReads = {
    ((__ \ "name").read[String] ~
    (__ \ "timestamp").read[DateTime] ~
    (__ \ "current").read[Double] ~
    (__ \ "state").read[String] ~
    (__ \ "location").read[String] ~
    (__ \ "type").read[String] ~
    (__ \ "current_alert").read[Double]) (MachineStatus.apply _)
  }
}

