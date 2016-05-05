package repository

import javax.inject.Inject
import javax.inject.Singleton

import io.getquill._
import io.getquill.naming.SnakeCase


import api.model._
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

  lazy val db = source(new CassandraAsyncSourceConfig[SnakeCase]("db"))

  val insert = quote {
    (id: UUID, name: String, current: Double, timestamp: DateTime) =>

    query[MachineData](
      _.entity("samples")).insert(
      _.id -> id,
      _.name -> name,
      _.current -> current,
      _.timestamp -> timestamp
    ).usingTtl(`24hours`)
  }

  def save(md: MachineData): Future[MachineData] = {
    db.run(insert)(
      md.id,
      md.name,
      md.current,
      md.timestamp)
    .map(_ => md)
  }
}
