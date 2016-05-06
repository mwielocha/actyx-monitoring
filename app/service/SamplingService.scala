package service

import java.util.UUID

import scala.language.postfixOps

import javax.inject.Inject
import javax.inject.Singleton

import akka.actor.ActorSystem
import scala.concurrent.ExecutionContext
import akka.stream.impl.fusing.GraphStages.TickSource
import scala.concurrent.duration._
import akka.stream.SourceShape
import akka.stream.scaladsl._
import akka.stream.ClosedShape
import akka.stream.OverflowStrategy
import akka.stream.FlowShape
import akka.stream.SinkShape

import akka.stream.Supervision

import akka.stream.ActorMaterializerSettings
import akka.stream.ActorMaterializer

import play.api.Logger

import actors.MachineSampler
import model._
import repository.SamplesRepository

/**
 * Created by Mikolaj Wielocha on 04/05/16
 */

@Singleton
class SamplingService @Inject() (private val client: MachineParkApiClient, private val samplesRepository: SamplesRepository)(implicit
  private val actorSystem: ActorSystem,
  private val ec: ExecutionContext) {

  val logger: Logger = Logger(this.getClass())

  val supervision: Supervision.Decider = {
    case e: Exception => logger.error("Error", e); Supervision.Restart
  }

  val rate = 5 seconds

  val offset = 10

  def connect(sink: Sink[Sample, _], machineId: UUID): Unit = {

    implicit val mat = ActorMaterializer(
        ActorMaterializerSettings(actorSystem)
          .withSupervisionStrategy(supervision))

    val flow = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder =>

      import GraphDSL.Implicits._

      val throttler = Source.tick[Unit](rate, rate, () => ())

      val sampler = client.newMachineInfoSource(machineId)

      val zip1 = builder.add(ZipWith[Unit, MachineInfo, MachineInfo]((_, md) => md))

      val zip2 = builder.add(ZipWith[MachineInfo, Double, Sample]((mi, avg) => Sample(mi, avg)))

      val splitter = builder.add(Broadcast[MachineInfo](2))

      val store = builder.add(Flow[Sample].mapAsync(1)(samplesRepository.save(_)))

      val average = builder
        .add(Flow[MachineInfo]
          .map(_.status.current).sliding(offset)
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
      
      zip2.out ~> store ~> Sink.foreach[Sample](println)

      ClosedShape
    })

    flow.run()
  }
}
