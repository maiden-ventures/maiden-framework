package maiden.auth.token

import shapeless._
import io.finch.{Endpoint,_}
import com.twitter.util.Future
import io.finch.Output.payload
import maiden.util.error.Errors._
import maiden.auth.{MaidenAuth, MaidenAuthUser, AnonymousAuthUser}
import maiden.config.MaidenConfig
import maiden.processing.Validations._

trait TokenAuth extends MaidenAuth {

  lazy val paramToken = MaidenConfig.get[String]("app.security.param_name")
  lazy val headerToken = MaidenConfig.get[String]("app.security.header_name")
  lazy val accessToken = MaidenConfig.get[String]("app.security.access_token")


  def authorize: Endpoint[MaidenAuthUser]  = {
    val auth = (paramOption(paramToken).as[String] :: headerOption(headerToken).as[String])

    auth.mapOutputAsync { tok =>
      println(tok)
      if (!tok(0).isEmpty && tok(0) == Option(accessToken)) authorized(AnonymousAuthUser())
      else if (!tok(1).isEmpty && tok(1) == Option(accessToken)) authorized(AnonymousAuthUser())
      else unauthorized
    }
  }

  def authorized(u: MaidenAuthUser): Future[Output[MaidenAuthUser]] =
    Future.value(payload(u))

  def unauthorized: Future[Output[AnonymousAuthUser]] =
     Future.value(Unauthorized(authFailedError(s"Missing or incorrect authorization info")))
}

object TokenAuth extends TokenAuth
