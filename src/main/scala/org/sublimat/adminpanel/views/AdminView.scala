package org.sublimat.adminpanel.views

import korolev.{Component, Context}
import korolev.effect.Effect
import korolev.state.javaSerialization._
import org.sublimat.Layers.AppEnv
import zio.ZIO


object AdminView {
  type ZF[A] = ZIO[AppEnv, Throwable, A]

  sealed trait AdminViewPage

  object AdminViewPage {

    case class AdminEditState() extends AdminViewPage

    case object AdminAddState extends AdminViewPage

    case class AdminListState() extends AdminViewPage

  }

  case class AdminViewState(s: AdminViewPage)

}

import AdminView._

class AdminView(implicit eff: Effect[ZF[*]]) {

  import AdminViewPage._

  val page = Component[ZF[*], AdminViewState, String, Any](AdminViewState(AdminListState())) {
    case (context, params, state) =>

      import context._
      import levsha.dsl._
      import html._

      div(
        div(h1("admin zone")),
        div(
          state match {
            case AdminViewState(AdminListState()) =>
              div(
                table (
                  tr(th("id"), th("name"), th("url"), th("edit")),
                ),
                button("Add", event("click")(access =>
                  access.transition(s => s.copy(s = AdminAddState)))
                )
              )

            case AdminViewState(AdminEditState()) =>
              div()

            case AdminViewState(AdminAddState) =>
              div()
          }
        )
      )
  }

}
