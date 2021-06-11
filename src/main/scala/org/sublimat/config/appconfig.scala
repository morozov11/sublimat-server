package org.sublimat

import com.typesafe.config.ConfigFactory
import org.sublimat.config.{ConfigLoader, HttpConfig}
import zio._

import scala.util.Try

package object appconfig {

  type HasAppConfig = Has[AppConfig.Config]

  object AppConfig {

    final case class Config(http: HttpConfig.Config)

    val live: ZLayer[Any, IllegalStateException, HasAppConfig] =
      ZIO
        .fromTry {
          Try {
            val config = ConfigFactory.load
            ConfigLoader.load(config)
          }
        }
        .mapError(failures =>
          new IllegalStateException(s"Error loading configuration: $failures")
        ).toLayer

    val getAppConfig: ZIO[HasAppConfig, Nothing, Config] = ZIO.environment[HasAppConfig].map(_.get)
  }
}
