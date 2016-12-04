package io.mwielocha.actyxapp.app

import javax.inject.{Inject, Named, Singleton}

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.HttpExt
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import io.mwielocha.actyxapp.actors.WebsocketRegistryActor
import io.mwielocha.actyxapp.controllers.AppController
import io.mwielocha.actyxapp.service.{MachineParkApiClient, SamplingService}
import io.mwielocha.actyxapp.util.ScalaLogging

/**
  * Created by mwielocha on 04/12/2016.
  */

@Singleton
class Bootstrap @Inject()(
  private val http: HttpExt,
  private val client: MachineParkApiClient,
  private val appController: AppController,
  private val samplingService: SamplingService,
  @Named(WebsocketRegistryActor.actorName) val websocketRegistryActor: ActorRef
)(
  implicit private val actorSystem: ActorSystem,
  private val actorMaterializer: ActorMaterializer
) extends ScalaLogging {

  import actorSystem.dispatcher

  def run(): Unit = {

    logger.info("Starting flow...")

    client.machines.foreach { machines =>
      samplingService.newMonitoringSource(machines)
        .runWith(Sink.actorRef(websocketRegistryActor, ()))
    }

    logger.info("Starting http...")

    http.bindAndHandle(appController(), "localhost", 9001)
  }
}
