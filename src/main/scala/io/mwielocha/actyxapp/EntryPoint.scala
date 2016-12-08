package io.mwielocha.actyxapp

import com.google.inject.Guice
import com.typesafe.config.ConfigFactory
import io.mwielocha.actyxapp.app.{AppModule, Bootstrap}
import net.codingwell.scalaguice.InjectorExtensions._

/**
  * Created by mwielocha on 04/12/2016.
  */
object EntryPoint {

  def main(args: Array[String]): Unit = {

    val config = ConfigFactory.defaultApplication()

    Guice.createInjector(new AppModule(config))
      .instance[Bootstrap].run()

  }
}
