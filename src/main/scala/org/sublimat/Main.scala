package org.sublimat


import zio.clock.Clock
import zio.{App, RIO, ZEnv, ZIO, logging, ExitCode => ZExitCode}
import org.sublimat.Layers.AppEnv
import org.sublimat.adminpanel.AdminEndpoint
import org.sublimat.appconfig.{AppConfig, HasAppConfig}
import zhttp.http.HttpApp
import zhttp.service.Server
import zio.blocking.Blocking


object Main extends App {

  type AppTask[A] = RIO[AppEnv, A]

  override def run(args: List[String]): ZIO[ZEnv, Nothing, ZExitCode] = {

    val prog = for {
      _   <- logging.log.info(s"Load configuration ...")
      cfg <- AppConfig.getAppConfig
      _   <- logging.log.info(s"Starting service ...")
      httpApp <- AdminEndpoint.route()
      _   <- logging.log.info(s"Run http at port ${cfg.http.port}")
      _   <- runHttp(httpApp, cfg.http.port)
    } yield ZExitCode.success

    prog.provideCustomLayer(Layers.appEnv).orDie

  }



  def runHttp[R <: Clock with Blocking](
                           httpApp: HttpApp[R, Throwable],
                           port: Int
                         ): ZIO[R, Throwable, Unit] = {
    Server.start(port, httpApp)
  }

}
