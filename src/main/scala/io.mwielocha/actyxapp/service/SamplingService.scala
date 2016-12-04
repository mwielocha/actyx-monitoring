package io.mwielocha.actyxapp.service

import java.util.UUID
import javax.inject.{Inject, Singleton}

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{FlowShape, Graph, SourceShape}
import akka.stream.scaladsl._
import io.mwielocha.actyxapp.util.ScalaLogging
import io.mwielocha.actyxapp.model._
import io.mwielocha.actyxapp.repository.SampleRepository

import scala.concurrent.duration._
import scala.language.postfixOps

/**
 * Created by Mikolaj Wielocha on 04/05/16
 */

@Singleton
class SamplingService @Inject() (
  private val client: MachineParkApiClient,
  private val samplesRepository: SampleRepository
)(implicit
  private val actorSystem: ActorSystem
) extends ScalaLogging {

  import actorSystem.dispatcher

  val offset = 60 // 5 minutes average

  private val machineMonitoringFlow = Flow.fromGraph(GraphDSL.create() { implicit builder =>

    import GraphDSL.Implicits._

    val throttler = Source.tick[Unit](0 seconds, 5 seconds, () => ())

    val zipper1 = builder.add(ZipWith[Unit, MachineInfo, MachineInfo]((_, md) => md))

    val zipper2 = builder.add(ZipWith[MachineInfo, Double, MachineInfo] {
      (mi, avg) => mi.copy(averageCurrent = avg)
    })

    val splitter = builder.add(Broadcast[MachineInfo](2))

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

    FlowShape(zipper1.in1, zipper2.out)
  })

  val envMonitoringFlow = Flow.fromGraph(GraphDSL.create() { implicit builder =>

    import GraphDSL.Implicits._

    val throttler = Source.tick[Unit](0 seconds, 1 minute, () => ())

    val zipper = builder.add(ZipWith[Unit, EnvInfo, EnvInfo]((_, md) => md))

    val compensate: FlowShape[EnvInfo, EnvInfo] = builder
      .add(Flow[EnvInfo]
        .expand(Iterator.continually(_)))

    throttler ~> zipper.in0

    zipper.out ~> compensate

    FlowShape(zipper.in1, compensate.out)
  })

  def newMachineMonitoringSource(machineId: UUID): Source[MachineInfo, _] = {
    Source.fromGraph(GraphDSL.create() { implicit builder =>

      import GraphDSL.Implicits._

      val flow = builder.add(machineMonitoringFlow)

      client.machineInfoSource(machineId) ~> flow

      SourceShape(flow.out)
    })
  }

  def newMachinesMonitoringSource(machines: List[UUID]): Source[MachineInfo, _] = {
    Source.fromGraph(GraphDSL.create() { implicit builder =>

      import GraphDSL.Implicits._

      val merger = builder.add(Merge[MachineInfo](machines.size))

      machines.foreach { machineId =>
        merger <~ newMachineMonitoringSource(machineId)
      }

      SourceShape(merger.out)
    })
  }

  def newEnvironmentMonitoringSource: Source[EnvInfo, _] = {
    Source.fromGraph(GraphDSL.create() { implicit builder =>

      import GraphDSL.Implicits._

      val flow = builder.add(envMonitoringFlow)

      client.envInfoSource ~> flow

      SourceShape(flow.out)
    })
  }

  def newMonitoringGraph(machines: List[UUID]): Graph[SourceShape[MachineInfo], NotUsed] = {

    GraphDSL.create() { implicit builder =>

      import GraphDSL.Implicits._

      val zipper = builder.add(ZipWith[MachineInfo, EnvInfo, MachineWithEnvInfo] {

        (m, e) => MachineWithEnvInfo(m, e)
      })

      val perister = Sink.foreachParallel[MachineWithEnvInfo](1)(samplesRepository.save(_))

      val splitter = builder.add(Broadcast[MachineInfo](2))

      newMachinesMonitoringSource(machines) ~> splitter

      splitter ~> zipper.in0

      newEnvironmentMonitoringSource ~> zipper.in1

      zipper.out ~> perister

      SourceShape(splitter.out(1))
    }
  }

  def newMonitoringSource(machines: List[UUID]): Source[MachineInfo, _] = {
    Source.fromGraph(newMonitoringGraph(machines))
  }
}