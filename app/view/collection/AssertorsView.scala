package org.w3.vs.view.collection

import org.w3.vs.view.model.{AssertionView, AssertorView}
import org.w3.vs.model.{Error, Warning}
import play.api.templates.Html
import Collection._
import play.api.i18n.Messages

case class AssertorsView(
    source: Iterable[AssertorView],
    id: String = "assertors",
    classe: String = "tabs",
    params: Parameters = Parameters()) extends CollectionImpl[AssertorView] {

  def copyWith(params: Parameters) = copy(params = params)

  def definitions: Seq[Definition] = Seq(
    ("name" -> true),
    ("errors" -> true),
    ("warnings" -> true)
  ).map(a => Definition(a._1, a._2))

  def emptyMessage: Html = Html("")

  def filter(filter: Option[String]): (AssertorView => Boolean) = _ => true

  def defaultSortParam = SortParam("", ascending = true)

  def order(sort: SortParam): Ordering[AssertorView] =
    Ordering[(Int, Int, String)].on[AssertorView](v => (-v.errors, -v.warnings, Messages(v.name)))

  def search(search: Option[String]): (AssertorView => Boolean) = _ => true

  def template: Option[Html] = {
    Some(views.html.template.assertor())
  }

}

object AssertorsView {

  def apply(assertionsView: Collection[AssertionView]): AssertorsView = {
    val views = assertionsView.source.groupBy(_.assertor).map {
      case (assertor, assertions) => {
        val errors = assertions.foldLeft(0) {
          case (count, assertion) =>
            count + (assertion.severity match {
              case Error => scala.math.max(assertion.contexts.size, 1)
              case _ => 0
            })
        }
        val warnings = assertions.foldLeft(0) {
          case (count, assertion) =>
            count + (assertion.severity match {
              case Warning => scala.math.max(assertion.contexts.size, 1)
              case _ => 0
            })
        }
        AssertorView(
          assertor,
          errors,
          warnings,
          assertionsView
        )
      }
    }
    new AssertorsView(views)
  }

}