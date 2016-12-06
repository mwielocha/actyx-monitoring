package io.mwielocha.actyxapp

import com.google.inject.Guice
import io.mwielocha.actyxapp.app.{AppModule, Bootstrap}
import net.codingwell.scalaguice.InjectorExtensions._

/**
  * Created by mwielocha on 04/12/2016.
  */
object EntryPoint extends App {

  Guice.createInjector(new AppModule)
    .instance[Bootstrap].run()
}
