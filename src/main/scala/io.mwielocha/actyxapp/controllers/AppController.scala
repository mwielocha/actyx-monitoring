package io.mwielocha.actyxapp.controllers

import java.util.UUID
import javax.inject.{Inject, Named}

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.ContentTypeResolver
import akka.stream.scaladsl.{Flow, GraphDSL, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy, SourceShape}
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import io.mwielocha.actyxapp.actors.WebsocketRegistryActor
import io.mwielocha.actyxapp.actors.WebsocketRegistryActor.Register
import io.mwielocha.actyxapp.model.MachineInfo
import io.mwielocha.actyxapp.service.{MachineParkApiClient, SamplingService}
import io.mwielocha.actyxapp.util.ScalaLogging
import play.api.libs.json.Json

/**
  * Created by mwielocha on 04/12/2016.
  */
class AppController @Inject() (
  private val client: MachineParkApiClient,
  private val samplingService: SamplingService,
  @Named(WebsocketRegistryActor.actorName) val websocketRegistryActor: ActorRef
)(
  implicit private val actorSystem: ActorSystem,
  private val actorMaterializer: ActorMaterializer
) extends PlayJsonSupport with ScalaLogging {

  import ContentTypeResolver.Default
  import actorSystem.dispatcher

  private val host = Option(System.getenv("HTTP_HOST"))

  private def websocketSourceGraph(machines: List[UUID]) = {

    GraphDSL.create() { implicit builder =>

      import GraphDSL.Implicits._

      val source = builder.add {
        Source.actorRef(100, OverflowStrategy.dropHead)
          .mapMaterializedValue(websocketRegistryActor ! Register(_))
      }

      val serializer = builder.add {
        Flow[MachineInfo].map {
          info => TextMessage(
            Json.toJson(info)
              .toString())
        }
      }

      source ~> serializer

      SourceShape(serializer.out)
    }
  }

  def apply(): Route = {

    pathPrefix("assets" / ) {

      (get & extractUnmatchedPath) { path =>
        getFromResource(path.toString())
      }

    } ~ path("socket") {

      (get & extractUpgradeToWebSocket) { upgrade =>
        complete {
          client.machines.map { machines =>
            upgrade.handleMessagesWithSinkSource(
              Sink.ignore, websocketSourceGraph(machines))
          }
        }
      }

    } ~ (get & extractHost) { h =>

      complete {
        client.machines.map { machines =>
          HttpEntity(
            ContentTypes.`text/html(UTF-8)`,
            html.Dashboard.render(
              host.getOrElse(h),
              machines
            ).body
          )
        }
      }
    }
  }
}
