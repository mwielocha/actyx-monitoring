package io.mwielocha.actyxapp.service

import java.util.UUID
import javax.inject.{Inject, Singleton}

import akka.actor.ActorSystem
import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import akka.stream.scaladsl.{Flow, Source}
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
    http.cachedHostConnectionPool[Unit](host)
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

  private def extract[T](implicit unm: Unmarshaller[HttpResponse, T]) = {

    Flow[(Try[HttpResponse], Unit)].mapAsync[Option[T]](1) {

      case (Success(response), _) if response.status.isSuccess() =>

        Unmarshal(response).to[T].map(Some(_))

      case (Success(response), _) =>

        logger.error(s"Request error, response was: $response")

        Future.successful(None)

      case (Failure(e), _) =>

        logger.error("Request error", e)

        Future.successful(None)

    }.filterNot(_.isEmpty).map(_.get)
  }

  def envInfoSource: Source[EnvInfo, _] = {

    val element = HttpRequest(uri = envSensorUrl) -> ()

    Source.repeat(element)
      .via(actyxEnvConnPool)
      .via(extract[EnvInfo])
  }

  def machines: Future[List[UUID]] = {
    scalacache.cachingWithTTL(cacheKey)(3 minutes) {
      for {
        response <- http.singleRequest(HttpRequest(uri = machinesUrl))
        unmarshalled <- Unmarshal(response).to[List[String]]
        parsed = unmarshalled.flatMap(UUIDRegex.findFirstIn).map(UUID.fromString)
      } yield parsed
    }
  }

  def machineInfoSource(machineId: UUID): Source[MachineInfo, _] = {

    val element = HttpRequest(uri = s"$machineUrl/$machineId") -> ()

    Source.repeat(element)
      .via(actyxMachineConnPool)
      .via(extract[MachineStatus])
      .map(MachineInfo(machineId, _))
  }
}

