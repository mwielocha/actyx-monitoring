package service

import scala.concurrent.Future
import java.util.UUID

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import play.api.libs.ws._
import play.api.Logger
import play.api.libs.json.Json
import play.api.cache._

import model._
import org.joda.time.DateTime

import akka.stream.scaladsl.Source
import akka.NotUsed
import akka.actor.Props
import akka.stream.actor.ActorPublisher
import akka.actor.ActorLogging
import akka.stream.actor.ActorPublisherMessage._
import akka.stream.scaladsl.Flow

import akka.actor.Actor
import akka.actor.ActorSystem
import akka.pattern._
import akka.util.Timeout

import scala.concurrent.duration._
import scala.language.postfixOps


import actors._

import scala.Either


/**
 * Created by Mikolaj Wielocha on 04/05/16
 */

@Singleton
class MachineParkApiClient @Inject() (
  private val ws: WSClient,
  private val cache: CacheApi)(
  implicit private val ec: ExecutionContext,
  private val actorSystem: ActorSystem) {

  val logger = Logger(getClass)

  val UUIDRegex = "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}".r

  val machineUrl = "http://machinepark.actyx.io/api/v1/machine"

  val machineEnpointThrottler = actorSystem.actorOf(Throttler.props(15 millis))

  private implicit val timeout = Timeout(60 seconds)

  def getMachineInfo(machineId: UUID): Future[Either[Throwable, MachineInfo]] = {

    for {
      _ <- machineEnpointThrottler ? Throttler.RequestToken
      result <- ws.url(s"$machineUrl/$machineId").get().map {
        case response if response.status == 200 => Right(MachineInfo(machineId, response.json.as[MachineStatus]))
        case response => logger.error(response.body); Left(new RuntimeException(response.body))
      }
    } yield result
  }

  val machinesUrl = "http://machinepark.actyx.io/api/v1/machines"

  def getMachines: Future[Either[Throwable, List[UUID]]] = {

    cache.get[List[UUID]]("machines") match {
      case Some(machines) => Future.successful(Right(machines))
      case None =>

        ws.url(machinesUrl).get().map {
          
          case response if response.status == 200 =>
            val machines = response.json.as[List[String]].flatMap {
              url => UUIDRegex.findFirstIn(url)
            }.map(UUID.fromString)

            cache.set("machines", machines, 3 minutes)

            Right(machines)
            
          case response => logger.error(response.body); Left(new RuntimeException(response.body))
        }
      }
  }

  def newMachineInfoSource(machineId: UUID): Source[MachineInfo, _] = {
    Source.actorPublisher(Props(new AsyncPublisher[MachineInfo]( () => getMachineInfo(machineId))))
  } 

  val environmentalSensorUrl = "http://machinepark.actyx.io/api/v1/env-sensor"

  def getEnvironmentalInfo: Future[Either[Throwable, EnvironmentalInfo]] = {
    ws.url(environmentalSensorUrl).get().map {
      case response if response.status == 200 => Right(response.json.as[EnvironmentalInfo])
      case response => logger.error(response.body); Left(new RuntimeException(response.body))
     }
  }

  def newEnvironmentalInfoSource: Source[EnvironmentalInfo, _] = Source.actorPublisher {
    Props(new AsyncPublisher[EnvironmentalInfo](() => getEnvironmentalInfo))
  }

  private [MachineParkApiClient] class AsyncPublisher[T](getAsync: () => Future[Either[Throwable, T]])
      extends ActorPublisher[T] with ActorLogging {

    import context.dispatcher

    def receive = {

      case Request(n) =>

        getAsync().foreach {
          case Left(t) => onError(t)
          case Right(e) => onNext(e)
        }

      case Cancel => context.stop(self)
    }
  }
}

