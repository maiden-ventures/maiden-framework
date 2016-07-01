package maiden.auth.oauth2

import io.finch.{Endpoint,_}
import com.twitter.util.Future
import io.finch.Output.payload
import maiden.util.error.Errors._
import maiden.auth.{MaidenAuth, MaidenAuthUser, AnonymousAuthUser}
import maiden.config.MaidenConfig

trait Oauth2Auth extends MaidenAuth {

  def authorize: Endpoint[MaidenAuthUser] = ???

  def authorized(u: MaidenAuthUser): Future[Output[MaidenAuthUser]] =
    Future.value(payload(u))

  def unauthorized: Future[Output[AnonymousAuthUser]] =
    Future.value(Unauthorized(authFailedError("Invalid Oauth2Auth")))

}
