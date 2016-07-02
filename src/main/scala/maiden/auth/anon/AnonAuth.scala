package maiden.auth.anon

import io.finch.{Endpoint,_}
import com.twitter.util.Future
import io.finch.Output.payload
import maiden.util.error.Errors._
import maiden.auth.{MaidenAuth, MaidenAuthUser, AnonymousAuthUser}
import maiden.config.MaidenConfig


//an authorization that always succeeds
trait AnonAuth extends MaidenAuth {

  def authorize: Endpoint[MaidenAuthUser]  = paramOption("xxx").mapOutputAsync {maybeToken =>
    maybeToken.map( x => x) match {
      case _ => authorized(AnonymousAuthUser())
  }}

  def authorized(u: MaidenAuthUser): Future[Output[MaidenAuthUser]] =
    Future.value(payload(u))

  //this will never get called for AnonAuth
  def unauthorized: Future[Output[AnonymousAuthUser]] = ???//authorized(AnonymousAuthUser())

}

object AnonAuth extends AnonAuth
