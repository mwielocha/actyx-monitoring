package io.mwielocha.actyxapp

import scala.collection.immutable.Queue

/**
  * Created by mwielocha on 06/12/2016.
  */
package object util {

  case class QueueBufferSize(size: Int) extends AnyVal

  implicit class QueueBuffer[+A](queue: Queue[A]) {

    def enqueueWithMaxSize[B >: A](e: B)(implicit maxSize: QueueBufferSize): Queue[B] = {
      if(queue.size < maxSize.size) queue.enqueue(e) else {
        queue.drop(queue.size - maxSize.size + 1).enqueue(e)
      }
    }
  }
}
