package repository

import javax.inject.Inject
import javax.inject.Singleton

import io.getquill._
import io.getquill.naming.SnakeCase


import model.MachineWithEnvironmentalInfo
import org.joda.time.DateTime
import java.util.Date
import scala.concurrent.Future
import scala.concurrent.ExecutionContext

import scala.util.Success
import scala.util.Failure

import io.getquill.sources.cassandra.ops._

import java.util.UUID


/**
 * Created by Mikolaj Wielocha on 05/05/16
 */

@Singleton
class SamplesRepository @Inject() (implicit private val ec: ExecutionContext) {

  val `24hours` = quote(86400)

  implicit val encodeDateTime = mappedEncoding[DateTime, Date](_.toDate())
  implicit val decodeDateTime = mappedEncoding[Date, DateTime](new DateTime(_))

  case class Samples(id: UUID, name: String, current: Double,
    pressure: Double, humidity: Double, temperatur: Double, timestamp: DateTime)

  lazy val db = source(new CassandraAsyncSourceConfig[SnakeCase]("db"))

  val insert = quote {
    (id: UUID, name: String, current: Double,
    pressure: Double, humidity: Double, temperatur: Double, timestamp: DateTime) =>

    query[Samples].insert(
      _.id -> id,
      _.name -> name,
      _.current -> current,
      _.pressure -> pressure,
      _.humidity -> humidity,
      _.temperatur -> temperatur,
      _.timestamp -> timestamp
    ).usingTtl(`24hours`)
  }

  def save(e: MachineWithEnvironmentalInfo): Future[MachineWithEnvironmentalInfo] = {
    db.run(insert)(
      e.machineInfo.id,
      e.machineInfo.status.name,
      e.machineInfo.status.current,
      e.environmentalInfo.pressure.value,
      e.environmentalInfo.humidity.value,
      e.environmentalInfo.temperature.value,
      DateTime.now
    ).map(_ => e)
  }
}
