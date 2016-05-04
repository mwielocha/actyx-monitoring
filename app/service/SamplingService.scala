package service

import java.util.UUID

import scala.language.postfixOps

import javax.inject.Inject
import javax.inject.Singleton

import akka.actor.ActorSystem
import scala.concurrent.ExecutionContext
import akka.stream.impl.fusing.GraphStages.TickSource
import scala.concurrent.duration._
//import akka.stream.scaladsl._
import akka.stream.SourceShape
import akka.stream.scaladsl._
import akka.stream.ClosedShape
import akka.stream.OverflowStrategy


import akka.stream.ActorMaterializerSettings
import akka.stream.ActorMaterializer

import play.api.Logger

import actors.MachineDataSampler
import api.model._
import api.Client

/**
 * Created by Mikolaj Wielocha on 04/05/16
 */

@Singleton
class SamplingService @Inject() (private val client: Client)(implicit
  private val actorSystem: ActorSystem,
  private val ec: ExecutionContext) {

  val logger: Logger = Logger(this.getClass())

  implicit val mat = ActorMaterializer()

  val machineId = UUID.fromString("0e079d74-3fce-42c5-86e9-0a4ecc9a26c5")

  val rate = 1 seconds

  val pipe = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder =>

    import GraphDSL.Implicits._

    val throttler = Source.tick[Unit](rate, rate, () => ())

    val sampler = Source.actorPublisher(MachineDataSampler.props(machineId, client))

    val sink = Sink.foreach[MachineData](println)

    val zip1 = builder.add(ZipWith[Unit, MachineData, MachineData]((_, md) => md))

    //val zip2 = builder.add(ZipWith[MachineData, Option[Double], MachineData]((md, avgOpt) => md.copy(avgCurrent = avgOpt)))

    //val splitter = builder.add(Broadcast[MachineData](2))

    // val f = builder.add(Flow[MachineData].map(_.current).grouped(10).map(_.sum).map(Some(_)).conflate()

    throttler ~> zip1.in0

    sampler ~> zip1.in1

    // zip1.out ~> splitter ~> zip2.in0
    // 
    // splitter ~> f ~> zip2.in1
    // 
    // zip2.out ~> sink

    zip1.out ~> sink

    ClosedShape
  })

  pipe.run()
}
