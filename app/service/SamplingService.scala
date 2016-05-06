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

  val rate = 5 seconds

  val offset = 10

  private val machineMonitoringFlow = Flow.fromGraph(GraphDSL.create() { implicit builder =>

    import GraphDSL.Implicits._

    val throttler = Source.tick[Unit](rate, rate, () => ())

    val zipper1 = builder.add(ZipWith[Unit, MachineInfo, MachineInfo]((_, md) => md))

    val zipper2 = builder.add(ZipWith[MachineInfo, Double, MachineInfoWithAverageCurrent] {
      (mi, avg) => MachineInfoWithAverageCurrent(mi, avg)
    })

    val splitter = builder.add(Broadcast[MachineInfo](2))

    val perister = builder.add(Flow[MachineInfoWithAverageCurrent]
      .mapAsync(1)(samplesRepository.save(_)))

    val average = builder
      .add(Flow[MachineInfo]
        .map(_.status.current).sliding(offset)
        .map(x => x.sum / x.size.toDouble))

    val compensate: FlowShape[Double, Double] = builder
      .add(Flow[Double]
        .expand(Iterator.continually(_)))

    val zero = Source.single[Double](0.0)

    val merger = builder.add(MergePreferred[Double](1))

    throttler ~> zipper1.in0

    zipper1.out ~> splitter ~> zipper2.in0

    zero ~> merger.preferred

    splitter ~> average ~> merger ~> compensate ~> zipper2.in1

    zipper2.out ~> perister

    FlowShape(zipper1.in1, perister.out)
  })

  def newMachineMonitoringSource(machineId: UUID): Source[MachineInfoWithAverageCurrent, _] = {
    Source.fromGraph(GraphDSL.create() { implicit builder =>

      import GraphDSL.Implicits._

      val source = client.newMachineInfoSource(machineId)

      val flow = builder.add(machineMonitoringFlow)

      source ~> flow

      SourceShape(flow.out)
    })
  }

  def newMachinesMonitoringSource(machines: List[UUID]): Source[MachineInfoWithAverageCurrent, _] = {
    Source.fromGraph(GraphDSL.create() { implicit builder =>

      import GraphDSL.Implicits._

      val merger = builder.add(Merge[MachineInfoWithAverageCurrent](machines.size))

      machines.foreach { machineId =>
        merger <~ newMachineMonitoringSource(machineId)
      }

      SourceShape(merger.out)
    })
  }
}
