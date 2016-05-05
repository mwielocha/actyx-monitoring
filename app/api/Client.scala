package api

import scala.concurrent.Future
import java.util.UUID

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import play.api.libs.ws._

import model._
import org.joda.time.DateTime


/**
 * Created by Mikolaj Wielocha on 04/05/16
 */

@Singleton
class Client @Inject() (private val ws: WSClient)(implicit private val ec: ExecutionContext) {

  val apiUrl = "http://machinepark.actyx.io/api/v1/machine"

  def getMachineStatus(machineId: UUID): Future[MachineData] = {
    println("Get")
    // ws.url(s"$apiUrl/$machineId").get().map {
    //   case response if response.status == 200 => response.json.as[MachineData]
    //   case response => println(s"Error: ${response.body}"); throw new RuntimeException
    // }

    Future.successful(MachineData("Sample", DateTime.now, math.random))
  }
}

