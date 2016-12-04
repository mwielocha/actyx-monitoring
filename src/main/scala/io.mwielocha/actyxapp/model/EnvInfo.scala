package io.mwielocha.actyxapp.model

import org.joda.time.DateTime

import play.api.libs.json._


/**
 * Created by Mikolaj Wielocha on 06/05/16
 */

case class Metric(value: Double, timestampe: DateTime)

object Metric {

 implicit val jodaDateReads = Reads.jodaDateReads("yyyy-MM-dd'T'HH:mm:ss")

  implicit val reads = new Reads[Metric] {
    override def reads(js: JsValue): JsResult[Metric] = {
      js match {
        case array: JsArray =>
          JsSuccess(
            Metric(
              array.value.last.as[Double],
              array.value.head.as[DateTime])
          )
        case _ => JsError("Error on parsing metric") 
      }
    }
  }
}

case class EnvInfo(pressure: Metric, humidity: Metric, temperature: Metric)

object EnvInfo {

  implicit val reads = Json.reads[EnvInfo]

}




