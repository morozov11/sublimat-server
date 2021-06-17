package org.sublimat.config

import org.sublimat.appconfig.AppConfig

object ConfigLoader {

  def load(config: com.typesafe.config.Config): AppConfig.Config = {

    val http = HttpConfig.load(config.getConfig("http"))
    AppConfig.Config(http)
  }

}
