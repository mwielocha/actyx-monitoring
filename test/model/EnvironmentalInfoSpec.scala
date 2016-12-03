package model

import org.scalatest.FlatSpec
import org.scalatest.Matchers

import org.joda.time.DateTime

import play.api.libs.json.Json

/**
 * Created by Mikolaj Wielocha on 04/05/16
 */

class EnvironmentalInfoSpec extends FlatSpec with Matchers {

  "MachineData json reads" should "parse sample json" in {

    val raw = """{
	  "pressure": ["2016-05-06T16:10:00", 1004.01],
	  "temperature": ["2016-05-06T16:10:00", 16.829999999999998],
	  "humidity": ["2016-05-06T16:10:00", 91.319999999999993]
    }"""

    Json.parse(raw).as[EnvInfo] shouldBe EnvInfo(
      Metric(1004.01, DateTime.parse("2016-05-06T16:10:00")),
      Metric(91.319999999999993, DateTime.parse("2016-05-06T16:10:00")),
      Metric(16.829999999999998, DateTime.parse("2016-05-06T16:10:00"))
    )
  }
}

