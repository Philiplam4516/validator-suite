package org.w3.vs.actor.message

import akka.actor.ActorRef

case object Refresh
case object Stop

case object BeProactive
case object BeLazy

case object GetJobData
case object NoMorePendingAssertion