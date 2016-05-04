package api.model

import org.joda.time.DateTime


/**
 * Created by Mikolaj Wielocha on 04/05/16
 */

case class MachineData(name: String, timestamp: DateTime, current: Double, avgCurrent: Option[Double])

