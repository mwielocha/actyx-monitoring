package io.mwielocha.actyxapp.model

import org.scalatest.FlatSpec
import org.scalatest.Matchers

import org.joda.time.DateTime

import play.api.libs.json.Json


/**
 * Created by Mikolaj Wielocha on 04/05/16
 */

class MachineStatusSpec extends FlatSpec with Matchers {

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

    /*
       name: String,
  timestamp: DateTime,
  current: Double,
  state: String,
  location: String,
  machineType: String,
  currentAlert: Double) {
     */

    Json.parse(raw).as[MachineStatus] shouldBe MachineStatus(
      "DMG DMU 40eVo [#50]",
      DateTime.parse("2016-05-04T17:09:51.320410"),
      12.21,
      "working",
      "0.0,0.0",
      "mill",
      14.0)
  }
}

