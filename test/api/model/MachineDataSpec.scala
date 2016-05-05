package api.model

import org.scalatest.FlatSpec
import org.scalatest.Matchers

import org.joda.time.DateTime

import play.api.libs.json.Json


/**
 * Created by Mikolaj Wielocha on 04/05/16
 */

class MachineDataSpec extends FlatSpec with Matchers {

  "MachineData json reads" should "parse sample json" in {

    val raw = """{
      "type": "mill",
      "current_alert": 14.0,
      "location": "0.0,0.0",
      "state": "working",
      "current": 12.21,
      "timestamp": "2016-05-04T17:09:51.320410",
      "name": "DMG DMU 40eVo [#50]"
    }"""

    Json.parse(raw).as[MachineData] shouldBe MachineData(
      
      "DMG DMU 40eVo [#50]",
      DateTime.parse("2016-05-04T17:09:51.320410"),
      12.21)
  }
}

