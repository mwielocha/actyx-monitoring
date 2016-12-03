package service

import java.util.UUID
import javax.inject.{Inject, Singleton}

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, ResponseEntity}
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorMaterializer, ThrottleMode}
import akka.util.Timeout
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import model._
import play.api.cache._
import play.api.libs.ws._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.{Success, Try}


/**
 * Created by Mikolaj Wielocha on 04/05/16
 */

@Singleton
class MachineParkApiClient @Inject() (
  private val ws: WSClient,
  private val cache: CacheApi
)(
  implicit private val ec: ExecutionContext,
  private val actorSystem: ActorSystem
) extends PlayJsonSupport {

  private implicit val materializer = ActorMaterializer()

  private val http = Http(actorSystem)

  private val actyxConnPool = {
    http.newHostConnectionPool[Unit]("machinepark.actyx.io")
    .throttle(80, 1 second, 80, ThrottleMode.Shaping)
  }

  private val UUIDRegex = "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}".r

  private val machineUrl = "/api/v1/machine"

  private implicit val timeout = Timeout(60 seconds)

  private val machinesUrl = "/api/v1/machines"

  private val envSensorUrl = "/api/v1/env-sensor"

  private def extract[T](implicit unm: Unmarshaller[HttpResponse, T]) = {
    Flow[(Try[HttpResponse], Unit)].mapAsync[Option[T]](1) {
      case (Success(response), _) if response.status.isSuccess() =>
        Unmarshal(response).to[T].map(Some(_))
      case _ =>
        Future.successful(None)
    }.filterNot(_.isEmpty).map(_.get)
  }

  def envInfoSource: Source[EnvInfo, _] = {

    val element = HttpRequest(uri = envSensorUrl) -> ()

    Source.repeat(element)
      .via(actyxConnPool)
      .via(extract[EnvInfo])
  }

  def machines: Future[List[UUID]] = {

    val element = HttpRequest(uri = machinesUrl) -> ()

    val parse = Flow[List[String]].map {
      _.flatMap(UUIDRegex.findFirstIn)
        .map(UUID.fromString)
    }

    Source.single(element)
      .via(actyxConnPool)
      .via(extract[List[String]])
      .via(parse)
      .runWith(Sink.head)
  }

  def machineInfoSource(machineId: UUID): Source[MachineInfo, _] = {

    val element = HttpRequest(uri = s"$machineUrl/$machineId") -> ()

    Source.repeat(element)
      .via(actyxConnPool)
      .via(extract[MachineStatus])
      .map(MachineInfo(machineId, _))
  }
}

