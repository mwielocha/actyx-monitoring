package controllers

import javax.inject.Inject
import javax.inject.Singleton

import play.api.mvc.Controller
import play.api.mvc.Action

import service.SamplingService

import akka.stream.scaladsl.Sink
import model.MachineWithEnvironmentalInfo
import service.MachineParkApiClient
import scala.concurrent.ExecutionContext
import play.api.Logger

import akka.stream.Supervision

import akka.stream.ActorMaterializerSettings
import akka.stream.ActorMaterializer
import akka.actor.ActorSystem


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

  def main = Action.async {

    val sink = Sink.ignore

    client.getMachines.map { machines =>
      val source = samplingService.newMonitoringSource(machines)
      val flow = source to sink
      flow.run()
      Ok
    }
  }
}
