package actors

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Props

import scala.collection.mutable

/**
 * Created by Mikolaj Wielocha on 08/05/16
 */

class WebSocketRegistry extends Actor with ActorLogging {

  import WebSocketRegistry._

  val webSockets = new mutable.ArrayBuffer[ActorRef]()

  def receive = {
    case RegisterWebSocket(ws) => webSockets += ws
    case UnregisterWebSocket(ws) => webSockets -= ws
    case message => webSockets.foreach(_ ! message)
  }
}

object WebSocketRegistry {

  case class RegisterWebSocket(ws: ActorRef)
  case class UnregisterWebSocket(ws: ActorRef)

  def props = Props[WebSocketRegistry]

}


