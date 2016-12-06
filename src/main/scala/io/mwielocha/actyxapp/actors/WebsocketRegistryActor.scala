package io.mwielocha.actyxapp.actors

import akka.actor.{Actor, ActorRef, Terminated}
import io.mwielocha.actyxapp.util.ScalaLogging

import scala.collection._
import scala.collection.immutable.Queue
import io.mwielocha.actyxapp.util._

/**
  * Created by mwielocha on 04/12/2016.
  */

object WebsocketRegistryActor {

  implicit val bufferSize = QueueBufferSize(1000)

  final val actorName = "websocketRegistryActor"

  case class Register(actorRef: ActorRef)
}

class WebsocketRegistryActor extends Actor with ScalaLogging {

  import WebsocketRegistryActor._

  private val registry = mutable.Set.empty[ActorRef]

  private var buffer = Queue[Any]()

  override def receive = {

    case Register(ref) =>

      logger.debug("New websocket actor registered...")

      registry += context.watch(ref)

      buffer.foreach(ref ! _)

    case Terminated(ref) =>

      logger.debug("Websocket actor terminating...")

      registry -= context.unwatch(ref)

      logger.debug("Websocket actor unregistered...")

    case other =>

      buffer = buffer.enqueueWithMaxSize(other)

      registry.foreach(_ ! other)
  }
}
