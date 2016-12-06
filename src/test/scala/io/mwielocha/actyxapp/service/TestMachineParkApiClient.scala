package io.mwielocha.actyxapp.service

import java.util.UUID

import akka.stream.scaladsl.Source
import io.mwielocha.actyxapp.model.{EnvInfo, MachineInfo}

import scala.concurrent.Future

/**
  * Created by mwielocha on 06/12/2016.
  */

class TestMachineParkApiClient(
  private val envInfos: List[EnvInfo],
  private val machineInfos: List[MachineInfo]
) extends MachineParkApiClient {

  override def machines: Future[List[UUID]] = {
    Future.successful {
      machineInfos.map(_.id)
    }
  }

  override def envInfoSource: Source[EnvInfo, _] = {
    Source.fromIterator(() => envInfos.iterator)
  }

  override def allMachinesInfoSource(machines: List[UUID]): Source[MachineInfo, _] = {
    Source.fromIterator(() => machineInfos.iterator)
  }
}
