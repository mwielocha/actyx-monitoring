package service

import scala.concurrent.Future
import java.util.UUID

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import play.api.libs.ws._

import model._
import org.joda.time.DateTime

import akka.stream.scaladsl.Source
import akka.NotUsed
import akka.actor.Props
import akka.stream.actor.ActorPublisher
import akka.actor.ActorLogging
import akka.stream.actor.ActorPublisherMessage._


/**
 * Created by Mikolaj Wielocha on 04/05/16
 */

@Singleton
class MachineParkApiClient @Inject() (private val ws: WSClient)(implicit private val ec: ExecutionContext) {

  val UUIDRegex = "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}".r

  val machineUrl = "http://machinepark.actyx.io/api/v1/machine"

  def getMachineInfo(machineId: UUID): Future[MachineInfo] = {
    ws.url(s"$machineUrl/$machineId").get().map {
      case response => MachineInfo(machineId, response.json.as[MachineStatus])
    }
  }

  val machinesUrl = "http://machinepark.actyx.io/api/v1/machines"

  def getMachines: Future[List[UUID]] = {
    ws.url(machinesUrl).get().map {
      response => response.json.as[List[String]].flatMap {
        url => UUIDRegex.findFirstIn(url)
      }.map(UUID.fromString)
    }
  }

  def newMachineInfoSource(machineId: UUID): Source[MachineInfo, _] = Source.actorPublisher {
    Props(new MachineInfoPublisher(machineId))
  }

  private[MachineParkApiClient] class MachineInfoPublisher(private val machineId: UUID)
      extends ActorPublisher[MachineInfo] with ActorLogging {

    import context.dispatcher

    def receive = {

      case Request(n) =>

        getMachineInfo(machineId).foreach(onNext)

      case Cancel => context.stop(self)
    }
  }
}

