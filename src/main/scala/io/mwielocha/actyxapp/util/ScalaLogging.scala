package io.mwielocha.actyxapp.util

import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

/**
  * Created by mwielocha on 04/12/2016.
  */
trait ScalaLogging {

  protected val logger = Logger(LoggerFactory.getLogger(getClass))

  logger.info(s"[*** Create]: ${getClass.getSimpleName}")

}
