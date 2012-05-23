package org.w3.vs.model

import org.w3.vs.VSConfiguration
import org.w3.util._
import org.w3.util.Pimps._
import org.w3.vs.actor._
import scalaz._
import akka.dispatch._
import org.w3.vs.exception._

object User {
  
  def apply(
      id: UserId = UserId(),
      name: String,
      email: String,
      password: String,
      organizationId: OrganizationId): User =
    User(UserVO(id, name, email, password, organizationId))
  
  def get(id: UserId): FutureVal[Exception, User] = sys.error("")
  def getForOrganization(id: OrganizationId): FutureVal[Exception, Iterable[User]] = sys.error("") 
  
  // TODO: For now these only fail with StoreExceptions but should also fail with a Unauthorized exception 
  def authenticate(email: String, password: String)(implicit configuration: VSConfiguration): FutureVal[Exception, User] = {
    /*import configuration.store
    store.getUsers(email = Some(email), password = Some(password)).map(jobs => jobs.headOption).pureFold(
      f => Failure(f),
      {
        case Some(s) => Success(s)
        case _ => Failure(UnknownUser)
      }
    )*/
    sys.error("")
  }
  
  def getByEmail(email: String)(implicit configuration: VSConfiguration): FutureVal[Exception, User] = {
    /*import configuration.store
    store.getUsers(email = Some(email)).map(jobs => jobs.headOption).pureFold(
      f => Failure(f),
      {
        case Some(s) => Success(s)
        case _ => Failure(UnknownUser)
      }
    )*/
    sys.error("")
  }

}

case class UserVO(
    id: UserId = UserId(),
    name: String,
    email: String,
    password: String,
    organizationId: OrganizationId)

case class User(valueObject: UserVO) {
  def id: UserId = valueObject.id
  def name: String = valueObject.name
  def email: String = valueObject.email
  def password: String = valueObject.password
  
  def getOrganization: FutureVal[Exception, Organization] = sys.error("") 
}
