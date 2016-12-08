package io.mwielocha.actyxapp.actors

import javax.inject.Inject

import akka.actor.{Actor, ActorRef, Terminated}
import com.google.common.collect.EvictingQueue
import com.typesafe.config.Config
import io.mwielocha.actyxapp.util.ScalaLogging

import scala.collection.JavaConversions._
import scala.collection._

/**
  * Created by mwielocha on 04/12/2016.
  */

object WebsocketRegistryActor {

  final val actorName = "websocketRegistryActor"

  case class Register(actorRef: ActorRef)
}

class WebsocketRegistryActor @Inject()(
  private val config: Config
) extends Actor with ScalaLogging {

  import WebsocketRegistryActor._

  private val registry = mutable.Set.empty[ActorRef]

  private val buffer = EvictingQueue.create[Any](config.getInt("ws.bufferSize"))

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

      buffer.add(other)

      registry.foreach(_ ! other)
  }
}
