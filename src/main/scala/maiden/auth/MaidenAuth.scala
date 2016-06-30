package maiden.auth

import io.finch.{Endpoint, _}
import com.twitter.util.Future

//base trait for all authentication classes
trait MaidenAuth {

  val name: String = "Authentication Method"

  def authorize: Endpoint[MaidenAuthUser]

  def authorized(u: MaidenAuthUser): Future[Output[MaidenAuthUser]]

  def unauthorized: Future[Output[MaidenAuthUser]]

}

//base trait for all authentication endpoints
trait MaidenAuthEndpoint

trait MaidenAuthUser

case class AnonymousAuthUser() extends MaidenAuthUser
