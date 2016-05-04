package actors

import akka.stream.actor.ActorPublisher
import api.Client
import api.model._
import java.util.UUID
import akka.actor.Props

import akka.stream.actor.ActorPublisherMessage._

/**
 * Created by Mikolaj Wielocha on 04/05/16
 */

class MachineDataSampler(
  private val machineId: UUID,
  private val client: Client
) extends ActorPublisher[MachineData] {

  import context.dispatcher

  def receive = {

    case Request(_) =>

      client.getMachineStatus(machineId).foreach(onNext)

    case Cancel => context.stop(self)
  }
}

object MachineDataSampler {

  def props(machineId: UUID, client: Client) = Props(classOf[MachineDataSampler], machineId, client)

}
