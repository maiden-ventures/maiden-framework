package maiden.auth.token

import io.finch.{Endpoint,_}
import com.twitter.util.Future
import io.finch.Output.payload
import maiden.util.error.Errors._
import maiden.auth.{MaidenAuth, MaidenAuthUser, AnonymousAuthUser}
import maiden.config.MaidenConfig

trait TokenAuth extends MaidenAuth {

  lazy val paramToken = MaidenConfig.get[String]("app.security.param_name")
  lazy val accessToken = MaidenConfig.get[String]("app.security.access_token")

  def authorize: Endpoint[MaidenAuthUser]  =
    paramOption(paramToken).mapOutputAsync { maybeToken =>
      maybeToken match {
        case Some(t) if t == accessToken => authorized(AnonymousAuthUser())
        case _ => unauthorized
      }
    }

  def authorized(u: MaidenAuthUser): Future[Output[MaidenAuthUser]] =
    Future.value(payload(u))

  def unauthorized: Future[Output[AnonymousAuthUser]] =
    Future.value(Unauthorized(authFailedError(s"Missing auth token '${paramToken}' or value incorrect")))
}

object TokenAuth extends TokenAuth
