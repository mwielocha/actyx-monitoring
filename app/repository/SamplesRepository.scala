package repository

import java.util.{Date, UUID}
import javax.inject.Singleton

import io.getquill._
import model.MachineWithEnvInfo
import org.joda.time.DateTime

import scala.concurrent.{ExecutionContext, Future}


/**
 * Created by Mikolaj Wielocha on 05/05/16
 */

@Singleton
class SamplesRepository {

  private val ctx = new CassandraAsyncContext[SnakeCase]("db")

  import ctx._

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

  def save(e: MachineWithEnvInfo)(implicit ec: ExecutionContext): Future[MachineWithEnvInfo] = {

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
}
