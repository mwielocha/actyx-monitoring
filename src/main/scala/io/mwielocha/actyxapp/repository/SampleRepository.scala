package io.mwielocha.actyxapp.repository

import java.util.{Date, UUID}
import javax.inject.{Inject, Singleton}

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import io.getquill._
import io.mwielocha.actyxapp.model.MachineWithEnvInfo
import io.mwielocha.actyxapp.util.ScalaLogging
import org.joda.time.DateTime

import scala.concurrent.{ExecutionContext, Future}


/**
 * Created by Mikolaj Wielocha on 05/05/16
 */

trait SampleRepository {

  def persistingSink: Sink[MachineWithEnvInfo, _]

}

@Singleton
class DefaultSampleRepository @Inject()(
  private val actorSystem: ActorSystem,
  private val ctx: CassandraAsyncContext[SnakeCase]
) extends SampleRepository with ScalaLogging {

  import ctx._
  import actorSystem.dispatcher

  private val `24hours` = quote(86400)

  implicit private val encodeDateTime = MappedEncoding[DateTime, Date](_.toDate())
  implicit private val decodeDateTime = MappedEncoding[Date, DateTime](new DateTime(_))

  case class Sample(
    id: UUID,
    name: String,
    current: Double,
    pressure: Double,
    humidity: Double,
    temperature: Double,
    timestamp: DateTime
  )

  private def save(e: MachineWithEnvInfo)(implicit ec: ExecutionContext): Future[MachineWithEnvInfo] = {

    val sample = Sample(
      e.machineInfo.id,
      e.machineInfo.status.name,
      e.machineInfo.status.current,
      e.envInfo.pressure.value,
      e.envInfo.humidity.value,
      e.envInfo.temperature.value,
      DateTime.now
    )

    ctx.run {
      query[Sample]
        .insert(lift(sample))
        .usingTtl(`24hours`)
    }.map(_ => e)
  }

  override val persistingSink: Sink[MachineWithEnvInfo, _] = {
    Sink.foreachParallel[MachineWithEnvInfo](1)(save(_))
  }
}
