package org.sublimat.adminpanel

import korolev.Context
import korolev.effect.Effect
import korolev.server.{KorolevServiceConfig, StateLoader}
import zio.{RIO, Runtime, ZIO, logging}
import korolev.zio.{ZioEffect, zioEffectInstance}
import korolev.state.javaSerialization._
import korolev.ziohttp.ZioHttpKorolev
import org.sublimat.Layers.AppEnv
import org.sublimat.adminpanel.slf4j.Slf4jReporter
import org.sublimat.adminpanel.views.{AdminView, LoginView}
import zhttp.http.HttpApp

import scala.concurrent.ExecutionContext


class AdminEndpoint(implicit runtime: Runtime[AppEnv]) {

  implicit val ec: ExecutionContext = runtime.platform.executor.asEC

  implicit val effect= new ZioEffect[AppEnv, Throwable](runtime, identity, identity)

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

  def route(): HttpApp[AppEnv, Throwable] = {
    new ZioHttpKorolev[AppEnv]{}.http4sKorolevService(config)
  }
}

object AdminEndpoint {

  def route(): ZIO[AppEnv, Throwable, HttpApp[AppEnv, Throwable]] = {
    ZIO.runtime[AppEnv].flatMap { implicit rts =>
      for {
        _   <- logging.log.info(s"Starting admin service ...")
        result <- ZIO(new AdminEndpoint().route())
      } yield { result }
    }
  }

}
