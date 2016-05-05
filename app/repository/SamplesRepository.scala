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

/**
 * Created by Mikolaj Wielocha on 05/05/16
 */

@Singleton
class SamplesRepository @Inject() (implicit private val ec: ExecutionContext) {

  implicit val encodeDateTime = mappedEncoding[DateTime, Date](_.toDate())
  implicit val decodeDateTime = mappedEncoding[Date, DateTime](new DateTime(_))

  lazy val db = source(new CassandraAsyncSourceConfig[SnakeCase]("db"))

  val samples = quote {
    query[MachineData](
      _.entity("samples")
        .columns(
        _.id -> "id",
        _.name -> "name",
        _.current -> "current",
        _.timestamp -> "timestamp"))
  }

  val insert = quote(samples.insert)

  def save(md: MachineData): Future[MachineData] = {
    db.run(insert)(md).map(_ => md)
  }
}
