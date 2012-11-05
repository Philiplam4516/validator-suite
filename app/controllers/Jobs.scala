package controllers

import org.w3.vs.actor.message._
import org.w3.vs.exception._
import org.w3.vs.model._
import org.w3.vs.view.collection._
import org.w3.vs.view.form._
import org.w3.vs.view.model._
import play.Logger.ALogger
import play.api.libs.iteratee.Enumeratee
import play.api.libs.iteratee.Enumerator
import play.api.libs.iteratee.Iteratee
import play.api.libs.json.JsValue
import play.api.libs.{EventSource, Comet}
import play.api.mvc._
import scala.concurrent.ExecutionContext.Implicits.global
import org.w3.vs.controllers._
import scala.concurrent.Future

object Jobs extends VSController {
  
  val logger: ALogger = play.Logger.of("org.w3.vs.controllers.Jobs")

  def index: ActionA = AuthAsyncAction { implicit req => user =>
    for {
      jobs_ <- user.getJobs()
      jobs <- JobsView(jobs_)
    } yield {
      case _: Html => {
        Ok(views.html.main(
          user = user,
          title = "Jobs - Validator Suite",
          script = "test",
          collections = Seq(jobs.bindFromRequest)
        ))
      }
      case Json => Ok(jobs.bindFromRequest.toJson)
      case Rdf => TODO(req) // TODO
    }
  }

  def newJob: ActionA = AuthAction { implicit req => user => {
    case _: Html => Ok(views.html.jobForm(JobForm.blank, user, None))
  }}

  def create: ActionA = AuthAsyncAction { implicit req => user =>
    val result: Future[PartialFunction[Format, Result]] = (for {
      form <- Future.successful(JobForm.bind match {
        case Left(form) => throw new InvalidFormException(form)
        case Right(validJobForm) => validJobForm
      })
      job <- form.createJob(user).save()
    } yield {
      case _: Html => SeeOther(routes.Jobs.index()) /*.flashing(("success" -> Messages("jobs.created", job.name)))*/
      case _ => Created
    })
    result recover {
      case InvalidFormException(form: JobForm) => {
        case format: Html => BadRequest(views.html.jobForm(form, user, None))
        case _ => BadRequest
      }
    }
  }

  def socket(typ: SocketType): Handler = {
    typ match {
      case SocketType.ws => webSocket()
      case SocketType.events => eventsSocket()
      case SocketType.comet => cometSocket()
    }
  }

  def webSocket(): WebSocket[JsValue] = WebSocket.using[JsValue] { implicit reqHeader =>
    val iteratee = Iteratee.ignore[JsValue]
    val enum: Enumerator[JsValue] = Enumerator.flatten(getUser().map(user => enumerator(user)))
    (iteratee, enum)
  }

  def cometSocket: ActionA = AuthAction { implicit req => user => {
    case _: Html => Ok.stream(enumerator(user) &> Comet(callback = "parent.VS.jobupdate"))
  }}

  def eventsSocket: ActionA = AuthAction { implicit req => user => {
    case Stream => Ok.stream(enumerator(user) &> EventSource()) //.as("text/event-stream")
  }}

  private def enumerator(user: User): Enumerator[JsValue] = {
    Enumerator.flatten((
      for {
        org <- user.getOrganization() map (_.get)
      } yield {
        // ready to explode...
        // better: a user can belong to several organization. this would handle the case with 0, 1 and > 1
        org.enumerator &> Enumeratee.collect {
          case a: UpdateData => JobView.toJobMessage(a.jobId, a.data, a.activity)
          case a: RunCompleted => JobView.toJobMessage(a.jobId, a.completedOn)
        }
      }
    ).recover{ case _ => Enumerator.eof[JsValue] })
  }

}
