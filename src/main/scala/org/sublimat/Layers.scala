package org.sublimat

import org.sublimat.appconfig.{AppConfig, HasAppConfig}
import zio.blocking.Blocking
import zio.clock.Clock
import zio.logging.Logging
import zio.logging.slf4j.Slf4jLogger

object Layers {

  type AppEnv = Logging with Clock with Blocking with HasAppConfig


    val logging             = Slf4jLogger.make((_, msg) => msg)

    val appConfig = AppConfig.live
}
