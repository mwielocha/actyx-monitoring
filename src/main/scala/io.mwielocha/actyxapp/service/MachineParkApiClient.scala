package io.mwielocha.actyxapp.service

import java.util.UUID
import javax.inject.{Inject, Singleton}

import akka.actor.ActorSystem
import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorMaterializer, ThrottleMode}
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import io.mwielocha.actyxapp.model._
import io.mwielocha.actyxapp.util.ScalaLogging

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}
import scalacache.ScalaCache
import scalacache.serialization.InMemoryRepr


/**
 * Created by Mikolaj Wielocha on 04/05/16
 */

@Singleton
class MachineParkApiClient @Inject()(
  private val http: HttpExt
) (
  implicit
  private val cache: ScalaCache[InMemoryRepr],
  private val actorSystem: ActorSystem,
  private val actorMaterializer: ActorMaterializer
) extends PlayJsonSupport with ScalaLogging {

  import actorSystem.dispatcher

  private val host = "machinepark.actyx.io"

  private val actyxMachineConnPool = {
    http.cachedHostConnectionPool[UUID](host)
    .throttle(80, 1 second, 1, ThrottleMode.Shaping)
  }

  private val actyxEnvConnPool = {
    http.cachedHostConnectionPool[Unit](host)
      .throttle(3, 1 minute, 1, ThrottleMode.Shaping)
  }

  private val UUIDRegex = "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}".r

  private val machineUrl = "/api/v1/machine"

  private val machinesUrl = s"http://machinepark.actyx.io/api/v1/machines"

  private val envSensorUrl = "/api/v1/env-sensor"

  private val cacheKey = "machines"

  private def extract[T, C](implicit unm: Unmarshaller[HttpResponse, T]) = {

    Flow[(Try[HttpResponse], C)].mapAsync[Option[(T, C)]](1) {

      case (Success(response), ctx) if response.status.isSuccess() =>

        Unmarshal(response).to[T].map(json => Some(json -> ctx))

      case (Success(response), _) =>

        logger.error(s"Request error, response was: $response")

        response.entity.dataBytes.runWith(Sink.ignore)

        Future.successful(None)

      case (Failure(e), _) =>

        logger.error("Request error", e)

        Future.successful(None)

    }.filterNot(_.isEmpty).map(_.get)
  }

  def envInfoSource: Source[EnvInfo, _] = {

    val element = HttpRequest(uri = envSensorUrl) -> ()

    val source = Source.repeat(element)
      .via(actyxEnvConnPool)
      .via(extract[EnvInfo, Unit])

    source.map {
      case (info, _) => info
    }
  }

  def machines: Future[List[UUID]] = {
    scalacache.caching(cacheKey) {
      for {
        response <- http.singleRequest(HttpRequest(uri = machinesUrl))
        _ = if(response.status.isFailure()) logger.error(s"Error fething machines, response was: $response")
        unmarshalled <- Unmarshal(response).to[List[String]]
        parsed = unmarshalled.flatMap(UUIDRegex.findFirstIn).map(UUID.fromString)
      } yield parsed
    }
  }

  def allMachinesInfoSource(machines: List[UUID]): Source[MachineInfo, _] = {

    val source = Source.cycle(() => machines.iterator)
      .map(id => HttpRequest(uri = s"$machineUrl/$id") -> id)
      .via(actyxMachineConnPool)
      .via(extract[MachineStatus, UUID])

    source.map {
      case (status, id) =>
        MachineInfo(id, status)
    }
  }
}

