package actors

import akka.actor.FSM
import akka.actor.ActorLogging
import akka.actor.ActorRef


import scala.collection.immutable.Queue

import scala.concurrent.duration._
import akka.actor.Props




/**
 * Created by Mikolaj Wielocha on 08/05/16
 */

object Throttler {

  case object Token
  case object RequestToken

  private [Throttler] case object Awake

  sealed trait State
  case object Sleeping extends State
  case object Waiting extends State

  def props(rate: FiniteDuration) = Props(new Throttler(rate))

}

class Throttler(private val rate: FiniteDuration) extends FSM[Throttler.State, Queue[ActorRef]] with ActorLogging {

  import Throttler._
  import context.dispatcher

  startWith(Waiting, Queue.empty[ActorRef])

  when(Waiting) {

    case Event(RequestToken, _) =>
      log.debug("Got token request, responding...")
      sender() ! Token
      goto(Sleeping)

  }

  when(Sleeping) {

    case Event(RequestToken, queue) =>
      log.debug("Enqueing token request...")
      stay using(queue enqueue sender())

    case Event(Awake, queue) if queue.isEmpty =>
      log.debug("Waking up with empty queue...")
      goto(Waiting)

    case Event(Awake, queue) =>
      log.debug(s"Waking up with ${queue.size} pending requests...")
      queue.dequeue match {
        case (e, tail) =>
          e ! Token
          goto(Sleeping).using(tail)
      }
  }

  onTransition {
    case (_, Sleeping) =>
      log.debug(s"Scheduling awake in $rate")
      context.system.scheduler.scheduleOnce(rate, self, Awake)
  }
}

  
