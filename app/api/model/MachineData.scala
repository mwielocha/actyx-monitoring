package api.model

import org.joda.time.DateTime

import java.util.UUID


/**
 * Created by Mikolaj Wielocha on 04/05/16
 */

case class MachineData(id: UUID, name: String, timestamp: DateTime, current: Double)

