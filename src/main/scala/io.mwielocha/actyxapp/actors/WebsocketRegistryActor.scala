package io.mwielocha.actyxapp.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Terminated}
import io.mwielocha.actyxapp.util.ScalaLogging

import scala.collection._

/**
  * Created by mwielocha on 04/12/2016.
  */

object WebsocketRegistryActor {

  final val actorName = "websocketRegistryActor"

  case class Register(actorRef: ActorRef)
  case class Unregister(actorRef: ActorRef)
}

class WebsocketRegistryActor extends Actor with ScalaLogging {

  import WebsocketRegistryActor._

  private val registry = mutable.Set.empty[ActorRef]

  override def receive = {

    case Register(ref) =>

      logger.debug("New websocket actor registered...")

      context.watch(ref)
      registry += ref

    case Unregister(ref) =>

      context.unwatch(ref)
      registry -= ref

      logger.debug("Websocket actor unregistered...")

    case Terminated(ref) =>

      logger.debug("Websocket actor terminating...")

      self ! Unregister(ref)

    case other => registry.foreach(_ ! other)
  }
}
