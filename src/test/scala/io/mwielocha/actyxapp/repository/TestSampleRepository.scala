package io.mwielocha.actyxapp.repository

import akka.stream.scaladsl.Sink
import io.mwielocha.actyxapp.model.MachineWithEnvInfo
import io.mwielocha.actyxapp.util.ScalaLogging

/**
  * Created by mwielocha on 06/12/2016.
  */
class TestSampleRepository extends SampleRepository with ScalaLogging {
  override val persistingSink: Sink[MachineWithEnvInfo, _] = Sink.ignore
}
