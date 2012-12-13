package controllers

import java.net.URL
import org.w3.vs.model.{User, JobId}
import org.w3.vs.view.Helper
import org.w3.vs.view.collection._
import scala.concurrent.ExecutionContext.Implicits.global
import org.w3.vs.controllers._
import play.api.mvc.{WebSocket, Result, Handler}
import scala.concurrent.Future
import org.w3.util.Util._
import com.yammer.metrics.Metrics
import java.util.concurrent.TimeUnit.{ MILLISECONDS, SECONDS }
import play.api.libs.json.JsValue
import play.api.libs.iteratee.{Enumeratee, Enumerator, Iteratee}
import play.api.libs.{EventSource, Comet}
import org.w3.vs.actor.message.{NewAssertorResult, RunUpdate}

object Assertions extends VSController  {

  val logger = play.Logger.of("org.w3.vs.controllers.Assertions")

  def index(id: JobId, url: Option[URL]) : ActionA = url match {
    case Some(url) => index(id, url)
    case None => index(id)
  }

  val indexName = (new controllers.javascript.ReverseAssertions).index.name
  val indexTimer = Metrics.newTimer(Assertions.getClass, indexName, MILLISECONDS, SECONDS)

  def index(id: JobId): ActionA = AuthAsyncAction { implicit req => user =>
    val f: Future[PartialFunction[Format, Result]] = for {
      job_ <- user.getJob(id)
      assertions_ <- job_.getAssertions()
      job <- JobsView.single(job_)
    } yield {
      case Html(_) => {
        val assertors = AssertorsView(assertions_)
        val assertions = AssertionsView.grouped(assertions_, id).filterOn(assertors.firstAssertor).bindFromRequest
        Ok(views.html.main(
          user = user,
          title = s"""Report for job "${job_.name}" - By messages - Validator Suite""",
          style = "",
          script = "test",
          crumbs = Seq(job_.name -> ""),
          collections = Seq(
            job.withAssertions(assertions.groupBy("message")),
            assertors.withAssertions(assertions),
            assertions
          )))
      }
      case Json => {
        val assertions = AssertionsView.grouped(assertions_, id).bindFromRequest
        Ok(assertions.toJson)
      }
    }
    f.timer(indexName).timer(indexTimer)
  }

  val indexUrlName = indexName + "+url"
  val indexUrlTimer = Metrics.newTimer(Assertions.getClass, indexUrlName, MILLISECONDS, SECONDS)

  def index(id: JobId, url: URL): ActionA = AuthAsyncAction { implicit req => user =>
    val f: Future[PartialFunction[Format, Result]] = for {
      job_ <- user.getJob(id)
      assertions_ <- job_.getAssertions().map(_.filter(_.url == url))
    } yield {
      case Html(_) => {
        val assertors = AssertorsView(assertions_)
        val assertions = AssertionsView(assertions_, id, url).filterOn(assertors.firstAssertor).bindFromRequest
        val resource = ResourcesView.single(url, assertions, job_.id)
        Ok(views.html.main(
          user = user,
          title = s"Report for ${Helper.shorten(url, 50)} - Validator Suite",
          style = "",
          script = "test",
          crumbs = Seq(
            job_.name -> routes.Job.get(job_.id),
            Helper.shorten(url, 50) -> ""),
          collections = Seq(
            resource.withAssertions(assertions),
            assertors.withAssertions(assertions),
            assertions
        )))
      }
      case Json => {
        val assertions = AssertionsView(assertions_, id, url).bindFromRequest
        Ok(assertions.toJson)
      }
    }
    f.timer(indexUrlName).timer(indexUrlTimer)
  }

  def socket(jobId: JobId, url: Option[URL], typ: SocketType): Handler = {
    typ match {
      case SocketType.ws => webSocket(jobId, url)
      case SocketType.events => eventsSocket(jobId, url)
      case SocketType.comet => cometSocket(jobId, url)
    }
  }

  def webSocket(jobId: JobId, url: Option[URL]): WebSocket[JsValue] = WebSocket.using[JsValue] { implicit reqHeader =>
    val iteratee = Iteratee.ignore[JsValue]
    val enum: Enumerator[JsValue] = Enumerator.flatten(getUser().map(user => enumerator(jobId, url, user)))
    (iteratee, enum)
  }

  def cometSocket(jobId: JobId, url: Option[URL]): ActionA = AuthAction { implicit req => user => {
    case Html(_) => Ok.stream(enumerator(jobId, url, user) &> Comet(callback = "parent.VS.resourceupdate"))
  }}

  def eventsSocket(jobId: JobId, url: Option[URL]): ActionA = AuthAction { implicit req => user => {
    case Stream => Ok.stream(enumerator(jobId, url, user) &> EventSource())
  }}

  private def enumerator(jobId: JobId, url: Option[URL], user: User): Enumerator[JsValue] = {
    Enumerator.flatten(org.w3.vs.model.Job.get(jobId).map(job =>
      job.enumerator &> Enumeratee.collect[RunUpdate] {
        url match {
          case None => {
            case NewAssertorResult(result, run, now) => {
              AssertionsView.grouped(result.assertions, jobId).toJson
            }
          }
          case Some(url) => {
            case NewAssertorResult(result, run, now) if result.assertions.map(_.url).toList.contains(url) => {
              AssertionsView(run.assertions.filter(_.url == url), jobId, url).toJson
            }
          }
        }
      }/*.recover{ case _ => Enumerator.eof }*/ // Need help here
    ))
  }

}
