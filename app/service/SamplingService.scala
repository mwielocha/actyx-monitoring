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
import akka.stream.FlowShape


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

  val offset = 10

  val pipe = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder =>

    import GraphDSL.Implicits._

    val throttler = Source.tick[Unit](rate, rate, () => ())

    val sampler = Source.actorPublisher(MachineDataSampler.props(machineId, client))

    val zip1 = builder.add(ZipWith[Unit, MachineData, MachineData]((_, md) => md))

    val zip2 = builder.add(ZipWith[MachineData, Double, MachineDataWithAverage]((md, avg) => MachineDataWithAverage(md, avg)))

    val splitter = builder.add(Broadcast[MachineData](2))

    val average = builder
      .add(Flow[MachineData]
      .map(_.current).sliding(offset)
      .map(x => x.sum / x.size.toDouble))

    val compensate: FlowShape[Double, Double] = builder
      .add(Flow[Double]
      .expand(Iterator.continually(_)))

    val zero = Source.single[Double](0.0)

    val merge = builder.add(MergePreferred[Double](1))

    throttler ~> zip1.in0

    sampler ~> zip1.in1

    zip1.out ~> splitter ~> zip2.in0

    zero ~> merge.preferred
    
    splitter ~> average ~> merge ~> compensate ~> zip2.in1
    
    zip2.out ~> Sink.foreach[MachineDataWithAverage](println)

    ClosedShape
  })

  pipe.run()
}
