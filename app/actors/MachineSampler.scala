package actors

import akka.stream.actor.ActorPublisher
import service.MachineParkApiClient
import model._
import java.util.UUID
import akka.actor.Props

import akka.stream.actor.ActorPublisherMessage._
import akka.actor.ActorLogging
import service.MachineParkApiClient


/**
 * Created by Mikolaj Wielocha on 04/05/16
 */

class MachineSampler(
  private val machineId: UUID,
  private val client: MachineParkApiClient
) extends ActorPublisher[MachineInfo] with ActorLogging {

  import context.dispatcher

  def receive = {

    case Request(n) =>

      client.getMachineInfo(machineId).foreach(onNext)

    case Cancel => context.stop(self)
  }
}

object MachineSampler {

  def props(machineId: UUID, client: MachineParkApiClient) = Props(classOf[MachineSampler], machineId, client)

}
