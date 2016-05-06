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
    Props(new AsyncPublisher[MachineInfo](() => getMachineInfo(machineId)))
  }

  val environmentalSensorUrl = "http://machinepark.actyx.io/api/v1/env-sensor"

  def getEnvironmentalInfo: Future[EnvironmentalInfo] = {
    ws.url(environmentalSensorUrl).get().map(_.json.as[EnvironmentalInfo])
  }

  def newEnvironmentalInfoSource: Source[EnvironmentalInfo, _] = Source.actorPublisher {
    Props(new AsyncPublisher[EnvironmentalInfo](() => getEnvironmentalInfo))
  }

  private [MachineParkApiClient] class AsyncPublisher[T](getAsync: () => Future[T])
      extends ActorPublisher[T] with ActorLogging {

    import context.dispatcher

    def receive = {

      case Request(n) =>

        getAsync().foreach(onNext)

      case Cancel => context.stop(self)
    }
  }
}

