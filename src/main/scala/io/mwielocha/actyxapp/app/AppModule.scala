package io.mwielocha.actyxapp.app

import javax.inject.{Inject, Named, Singleton}

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{Actor, ActorRef, ActorSystem, OneForOneStrategy}
import akka.http.scaladsl.Http
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import com.google.common.cache.CacheBuilder
import com.google.inject.{Injector, Provides}
import io.getquill.{CassandraAsyncContext, SnakeCase}
import io.mwielocha.actyxapp.actors.WebsocketRegistryActor
import io.mwielocha.actyxapp.akkaguice.{GuiceAkkaActorRefProvider, GuiceAkkaExtension}
import io.mwielocha.actyxapp.repository.{DefaultSampleRepository, SampleRepository}
import io.mwielocha.actyxapp.service.{DefaultMachineParkApiClient, MachineParkApiClient}
import io.mwielocha.actyxapp.util.ScalaLogging
import net.codingwell.scalaguice.ScalaModule

import scalacache._
import scalacache.guava._
import scalacache.serialization.InMemoryRepr

/**
  * Created by mwielocha on 04/12/2016.
  */
class AppModule extends ScalaModule with GuiceAkkaActorRefProvider with ScalaLogging {

  override def configure(): Unit = {

    bind[MachineParkApiClient]
      .to[DefaultMachineParkApiClient]

    bind[SampleRepository]
      .to[DefaultSampleRepository]

    bind[Actor]
      .annotatedWithName(WebsocketRegistryActor.actorName)
      .to[WebsocketRegistryActor]

  }

  @Provides
  @Singleton
  def cache: ScalaCache[InMemoryRepr] = ScalaCache {
    GuavaCache {
      CacheBuilder
        .newBuilder()
        .build[String, Object]
    }
  }

  @Provides
  @Singleton
  def actorSystem(@Inject() injector: Injector): ActorSystem = {
    val system = ActorSystem()
    GuiceAkkaExtension(system)
      .initialize(injector)
    system
  }

  @Provides
  @Singleton
  def http(implicit @Inject() actorSystem: ActorSystem) = Http()

  @Provides
  @Singleton
  def actorMaterializer(implicit @Inject() actorSystem: ActorSystem) = {

    val decider: Supervision.Decider = {
      e => logger.error("Stream error, restarting...", e)
        Supervision.Restart
    }

    ActorMaterializer {
      ActorMaterializerSettings(actorSystem)
        .withSupervisionStrategy(decider)
    }
  }

  @Provides
  @Singleton
  def cassandraContext = new CassandraAsyncContext[SnakeCase]("db")

  @Provides
  @Singleton
  @Named(WebsocketRegistryActor.actorName)
  def websocketRegistryActor(@Inject() actorSystem: ActorSystem): ActorRef = {
    provideActorRef(actorSystem, WebsocketRegistryActor.actorName)
  }
}
