package model

import java.util.UUID


/**
 * Created by Mikolaj Wielocha on 05/05/16
 */

case class MachineInfo(id: UUID, status: MachineStatus, averageCurrent: Double = 0.0)
