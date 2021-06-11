package org.sublimat.adminpanel.views

import korolev.Context
import levsha.Document
import org.sublimat.Layers.AppEnv
import org.sublimat.adminpanel.AppState
import zio.ZIO

import scala.concurrent.duration._
import scala.jdk.DurationConverters._


class LoginView(ctx: Context[ZIO[AppEnv, Throwable, *], AppState, Any]) {

  import ctx._
  import levsha.dsl._
  import html._

  private val loginField = elementId()
  private val passwordField = elementId()

  private def onSubmit(access: Access) = {
    for {
      _ <- ZIO.sleep(1.second.toJava)
      login <- access.valueOf(loginField)
      password <- access.valueOf(passwordField)
      loggedIn <- ZIO(login == "bububu" && password == "megakey")
      _ <- if(loggedIn) {
        access.transition {
              case AppState(false, _) =>
                AppState(true, false)
            }
      } else {
        for {
          _ <- access.transition { _ => AppState(false, true)}.fork
          _ <- ZIO.sleep(3.seconds.toJava)
          _ <- access.transition(_.copy(showInvalidLogin = false))
        } yield {}
      }

    } yield ()
  }

  def loginPage(state: AppState): Document.Node[Context.Binding[ZIO[AppEnv, Throwable, *], AppState, Any]] = {
    form(
      input(placeholder := "Login", loginField),
      input(placeholder := "Password", `type` := "password", passwordField),
      button("Log in"),
      when(state.showInvalidLogin)(div(
        backgroundColor @= "red",
        "Invalid credentials"
      )),
      event("submit")(onSubmit)
    )
  }


}
