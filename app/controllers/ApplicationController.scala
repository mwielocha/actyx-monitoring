package controllers

import javax.inject.Inject
import javax.inject.Singleton

import play.api.mvc.Controller
import play.api.mvc.Action
import play.api.libs.json.Json
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.WebSocket
import play.api.libs.json.JsValue
import play.api.libs.streams.ActorFlow

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import akka.stream.scaladsl.Sink
import akka.stream.Supervision
import akka.stream.ActorMaterializerSettings
import akka.stream.ActorMaterializer
import akka.actor.ActorSystem
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Props

import model.MachineInfo
import service.SamplingService
import service.MachineParkApiClient
import _root_.actors.WebSocketRegistry
import _root_.actors.WebSocketRegistry._

/**
 * Created by Mikolaj Wielocha on 04/05/16
 */

@Singleton
class ApplicationController @Inject() (
  private val client: MachineParkApiClient,
  val samplingService: SamplingService)
  (implicit private val actorSystem: ActorSystem,
    private val ec: ExecutionContext) extends Controller {

  val logger = Logger(getClass)

  private val supervision: Supervision.Decider = {
    case e: Exception => logger.error("Error", e); Supervision.Restart
  }

  private implicit val mat = ActorMaterializer(
    ActorMaterializerSettings(actorSystem)
      .withSupervisionStrategy(supervision))

  private val webSocketRegistry = actorSystem.actorOf(WebSocketRegistry.props)
  private val webSocketSink = Sink.actorRef(webSocketRegistry, ())

  def initialize() = {
    client.getMachines.foreach { machines =>
      val source = samplingService.newMonitoringSource(machines)
      val flow = source to webSocketSink
      flow.run()
    }
  }

  initialize()

  def main = Action.async {
    Future.successful(Ok)
  }

  def socket = WebSocket.accept[JsValue, JsValue] { request =>
    ActorFlow.actorRef(out => Props(new WebSocketActor(out, webSocketRegistry)))
  }

  class WebSocketActor(
    private val out: ActorRef,
    private val registry: ActorRef) extends Actor with ActorLogging {

    override def preStart(): Unit = {
      registry ! RegisterWebSocket(self)
    }

    override def postStop(): Unit = {
      registry ! UnregisterWebSocket(self)
    }

    def receive = {
      case e: MachineInfo => out ! Json.toJson(e)
    }
  }
}
