package model

import org.joda.time.DateTime

/**
 * Created by Mikolaj Wielocha on 05/05/16
 */

case class Sample(machineInfo: MachineInfo, averageCurrent: Double) {

  val timestamp: DateTime = DateTime.now

}
