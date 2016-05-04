package api

import play.api.libs.json.Json
import play.api.libs.json.Reads

/**
 * Created by Mikolaj Wielocha on 04/05/16
 */

package object model {

  implicit val jodaDateReads = Reads.jodaDateReads("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")

  implicit val machineDataReads = Json.reads[MachineData]

}
