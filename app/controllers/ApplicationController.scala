package controllers

import javax.inject.Inject
import javax.inject.Singleton

import play.api.mvc.Controller
import play.api.mvc.Action

import service.SamplingService

import akka.stream.scaladsl.Sink
import model.Sample
import service.MachineParkApiClient
import scala.concurrent.ExecutionContext
import play.api.Logger



/**
 * Created by Mikolaj Wielocha on 04/05/16
 */

@Singleton
class ApplicationController @Inject() (
  private val client: MachineParkApiClient,
  val samplingService: SamplingService)
  (private implicit val ec: ExecutionContext) extends Controller {

  val logger = Logger(getClass)

  def main = Action.async {

    val sink = Sink.foreach[Sample](println)

    client.getMachines.map { machines =>
      machines.foreach { machineId =>
        logger.info(s"Connecting to $machineId")
        samplingService.connect(sink, machineId)
      }
      Ok
    }
  }
}
