package org.sublimat.adminpanel

import cats.effect.ConcurrentEffect
import korolev.Context
import korolev.http4s.http4sKorolevService
import korolev.server.{KorolevServiceConfig, StateLoader}
import zio.{RIO, Runtime, ZIO, logging}
import korolev.zio.zioEffectInstance
import korolev.state.javaSerialization._
import org.http4s.HttpRoutes
import org.sublimat.Layers.AppEnv
import org.sublimat.adminpanel.AdmTypes.AdmTask
import org.sublimat.adminpanel.slf4j.Slf4jReporter
import org.sublimat.adminpanel.views.{AdminView, LoginView}

import scala.concurrent.ExecutionContext
import zio.interop.catz._


class AdminEndpoint(implicit runtime: Runtime[AppEnv]) {

  implicit val ec: ExecutionContext = runtime.platform.executor.asEC

  implicit val effect = zioEffectInstance[AppEnv, Throwable](runtime)(identity)(identity)

  private val ctx = Context[ZIO[AppEnv, Throwable, *], AppState, Any]

  import ctx._
  import AdmTypes._

  import levsha.dsl._
  import html._
  import scala.concurrent.duration._

  private def config = KorolevServiceConfig[AdmTask, AppState, Any](
    stateLoader = StateLoader.default(AppState(false, false)),
    rootPath = "/",
    document = initDocument,
    reporter = Slf4jReporter
  )

  val loginPage = new LoginView(ctx)
  val adminView = new AdminView()

  private def initDocument(state: AppState) = {
    Html(
      body(
        if(!state.loggedIn)(loginPage.loginPage(state))
        else adminView.page.silent("")
      )
    )
  }

  def route(): ZIO[AppEnv, Throwable, HttpRoutes[AdmTask]] = {
    RIO.concurrentEffectWith { implicit CE: ConcurrentEffect[AdmTask] =>
      ZIO(http4sKorolevService(config))
    }
  }
}

object AdminEndpoint {

  def route(): ZIO[AppEnv, Throwable, HttpRoutes[AdmTask]] = {
    ZIO.runtime[AppEnv].flatMap { implicit rts =>
      for {
        _   <- logging.log.info(s"Starting admin service ...")
        result <- new AdminEndpoint().route()
      } yield { result }
    }
  }

}
