package org.sublimat.config


object HttpConfig {

  final case class Config(port: Int, baseUrl: String)

  def load(cfg: com.typesafe.config.Config): Config = {
    Config(cfg.getInt("port"), cfg.getString("base-url"))
  }

}
