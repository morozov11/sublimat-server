package org.sublimat

import org.http4s.HttpApp
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.CORS
import zio.clock.Clock
import zio.{App, RIO, ZEnv, ZIO, logging, ExitCode => ZExitCode}
import zio.interop.catz._
import cats.effect.{ExitCode => CatsExitCode, _}
import org.http4s.implicits._
import org.sublimat.Layers.AppEnv
import org.sublimat.adminpanel.AdminEndpoint
import org.sublimat.appconfig.{AppConfig, HasAppConfig}
import zio.blocking.Blocking
import zio.magic.ZioProvideMagicOps

import scala.concurrent.ExecutionContext


object Main extends App {

  type AppTask[A] = RIO[AppEnv, A]

  override def run(args: List[String]): ZIO[ZEnv, Nothing, ZExitCode] = {

    val prog = for {
      _   <- logging.log.info(s"Load configuration ...")
      cfg <- AppConfig.getAppConfig
      _   <- logging.log.info(s"Starting service ...")
      wsRoute <- AdminEndpoint.route()
      httpApp = Router[AppTask]("/" -> wsRoute).orNotFound
      _   <- runHttp(httpApp, cfg.http.port)
    } yield ZExitCode.success

    prog.inject(
      Layers.appConfig,
      Layers.logging,
      Blocking.live,
      Clock.live
    ).orDie
  }



  def runHttp[R <: Clock with Blocking](
                           httpApp: HttpApp[RIO[R, *]],
                           port: Int
                         ): ZIO[R, Throwable, Unit] = {
    type Task[A] = RIO[R, A]
    ZIO.runtime[R].flatMap { implicit rts =>
      for {
        r <- BlazeServerBuilder
          .apply[Task](ExecutionContext.global)
          .bindHttp(port, "0.0.0.0")
          .withHttpApp(CORS(httpApp))
          .serve
          .compile[Task, Task, CatsExitCode]
          .drain
      } yield r
    }
  }

}
