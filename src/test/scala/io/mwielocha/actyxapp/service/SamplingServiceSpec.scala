package io.mwielocha.actyxapp.service

import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.scaladsl.{GraphDSL, Sink, Source}
import akka.stream.{ActorMaterializer, SourceShape}
import com.google.inject.Guice
import com.google.inject.util.Modules
import io.mwielocha.actyxapp.app.AppModule
import io.mwielocha.actyxapp.model.{EnvInfo, MachineInfo, MachineStatus, MachineWithEnvInfo, Metric}
import io.mwielocha.actyxapp.repository.{SampleRepository, TestSampleRepository}
import net.codingwell.scalaguice.InjectorExtensions._
import net.codingwell.scalaguice.ScalaModule
import org.joda.time.DateTime
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, MustMatchers}

import scala.collection.breakOut

/**
  * Created by mwielocha on 06/12/2016.
  */
class SamplingServiceSpec extends FlatSpec with MustMatchers with ScalaFutures {

  private val sampleSize = 60

  val envInfos: List[EnvInfo] = {

    val info = EnvInfo(
      pressure = Metric(1.0, DateTime.now),
      humidity = Metric(1.0, DateTime.now),
      temperature = Metric(1.0, DateTime.now)
    )

    List.fill(sampleSize)(info)
  }

  val machineInfos: List[MachineInfo] = {
    (1 to sampleSize).map { n =>
      MachineInfo(
        UUID.randomUUID(),
        MachineStatus(
          name = s"Test machine no. $n",
          timestamp = DateTime.now,
          current = 1 + math.random,
          state = "working",
          location = "0.0,0.0",
          machineType = "mill",
          currentAlert = 1.4
        )
      )
    }(breakOut)
  }

  private val machineInfosWithEnvInfos = {
    machineInfos.zip(envInfos).map {
      case (m, e) => MachineWithEnvInfo(m, e)
    }
  }

  private val machineIds = machineInfos.map(_.id)

  val machineStatuses: List[MachineStatus] = machineInfos.map(_.status)

  class TestAppModule extends ScalaModule {

    override def configure(): Unit = {

      bind[SampleRepository]
        .to[TestSampleRepository]

      bind[MachineParkApiClient]
        .toInstance(
          new TestMachineParkApiClient(
            envInfos, machineInfos))

    }
  }

  private val injector = Guice.createInjector(
    Modules.`override`(new AppModule)
      .`with`(new TestAppModule)
  )

  private val samplingService = injector.instance[SamplingService]

  private implicit val actorSystem = injector.instance[ActorSystem]

  private implicit val materializer = injector.instance[ActorMaterializer]

  "SamplingService pipeline" should "stream machine infos from out1" in {

    val graph = GraphDSL.create() { implicit builder =>

      import GraphDSL.Implicits._

      val sources = builder.add {
        samplingService
          .newMonitoringGraph(machineIds)
      }

      sources.out2 ~> Sink.ignore

      SourceShape(sources.out1)
    }

    val result = Source.fromGraph(graph)
      .runWith(Sink.seq)
      .futureValue

    result must contain theSameElementsAs machineInfos
  }

  it should "persist stream infos with env from out2" in {

    val graph = GraphDSL.create() { implicit builder =>

      import GraphDSL.Implicits._

      val sources = builder.add {
        samplingService
          .newMonitoringGraph(machineIds)
      }

      sources.out1 ~> Sink.ignore

      SourceShape(sources.out2)
    }

    val result = Source.fromGraph(graph)
      .runWith(Sink.seq)
      .futureValue

    result must contain theSameElementsAs machineInfosWithEnvInfos
  }

  it should "count average" in {

    val id = UUID.randomUUID()

    val input = (1 to 120).map { _ =>
      MachineInfo(
        id,
        MachineStatus(
          name = s"Test machine",
          timestamp = DateTime.now,
          current = 1 + math.random,
          state = "working",
          location = "0.0,0.0",
          machineType = "mill",
          currentAlert = 1.4
        )
      )
    }

    val throttler: Source[Unit, _] = Source.repeat(Unit)

    val result = {
      Source.fromIterator[MachineInfo](() => input.iterator)
        .via(samplingService.machineMonitoringFlow(throttler))
        .runWith(Sink.seq)
        .futureValue
    }

    input.take(60).map(_.status.current).sum / 60.0 mustBe {
      result.drop(60).head.averageCurrent
    }
  }
}
