package io.mwielocha.actyxapp.shapes

import akka.stream.{Inlet, Outlet, Shape}

import scala.annotation.unchecked.uncheckedVariance
import scala.collection.immutable.Seq

/**
  * Created by mwielocha on 06/12/2016.
  */
case class OutShape2[+O1, +O2](
  out1: Outlet[O1 @uncheckedVariance],
  out2: Outlet[O2 @uncheckedVariance]
) extends Shape {

  override def inlets: Seq[Inlet[_]] = Seq.empty

  override def outlets: Seq[Outlet[_]] = Seq(out1, out2)

  override def deepCopy(): Shape = OutShape2(out1.carbonCopy(), out2.carbonCopy())

  override def copyFromPorts(inlets: Seq[Inlet[_]], outlets: Seq[Outlet[_]]): Shape = {
    require(inlets.isEmpty, s"proposed inlets [${inlets.mkString(", ")}] do not fit OutShape2")
    require(outlets.size == 2, s"proposed outlets [${outlets.mkString(", ")}] do not fit OutShape2")
    OutShape2(outlets.head, outlets.last)
  }
}
