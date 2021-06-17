package org.sublimat.adminpanel.slf4j

import korolev.effect.Reporter
import org.slf4j.LoggerFactory

object Slf4jReporter extends Reporter {

  protected val logger = LoggerFactory.getLogger(getClass.getName)

  def error(message: String, cause: Throwable): Unit = logger.error(message, cause)

  def error(message: String): Unit = logger.error(message)

  def warning(message: String, cause: Throwable): Unit = logger.warn(message, cause)

  def warning(message: String): Unit = logger.warn(message)

  def info(message: String): Unit = logger.info(message)
}
