package maiden.social

import com.restfb.{DefaultFacebookClient, DefaultJsonMapper, Parameter, Version}
import com.restfb.types._
import maiden.common.HttpClient
import maiden.config.MaidenConfig
import maiden.exceptions._

class MaidenFacebook(accessToken: String) {

  private val client = try {
    new DefaultFacebookClient(accessToken, Version.VERSION_2_4)
  } catch {
    case e: Exception => throw (new FacebookFailureException(exc=Option(e)))
  }


 private def userToMap(user: Option[User]) = user match {
    case Some(u) => Map(
      "id" -> u.getId(),
      "firstName" -> u.getFirstName(),
      "lastName" -> u.getLastName(),
      "email" -> u.getEmail(),
      "bio" -> u.getBio(),
      "link" -> u.getLink(),
      "about" -> u.getAbout(),
      "birthday" -> u.getBirthday(),
      "gender" -> u.getGender(),
      "timezone" -> u.getTimezone(),
      "location" -> {
        if (u.getLocation() != null) {
          u.getLocation().getName()
        } else {
          ""
        }
      }
    )
    case _ => throw(new FacebookFailureException)
  }

  private def friendsToMap(friends: Option[List[NamedFacebookType]]) =  friends match {
    case Some(f) =>
      f.map(friend =>
        friend.getId().toString -> friend.getName().toString
      ).toMap
      Map[String, String]()
    case _ => Map[String, String]()
  }

  def getLogoutUrl(redirect: String) = {
    client.getLogoutUrl(redirect)
  }

  def getUser():Map[String, Any] = {
    val fields = "birthday,email,timezone,bio,first_name,last_name,link,about,gender,picture, location"
    val param = Parameter.`with`("fields", fields, new DefaultJsonMapper())
    val user = try {
      Option(client.fetchObject("me", classOf[User], param))
    } catch {
      case e: Exception => throw(new FacebookFailureException(exc = Option(e)))
    }
    userToMap(user)
  }


  def getFriends() = {
    import scala.collection.JavaConverters._
    val friends = try {
      //val param = Parameter.`with`("limit", 500, new DefaultJsonMapper())
      val f = client.fetchConnection("me/friends", classOf[User]/*, param*/)
      val fm = f.iterator().asScala.map(x => x)
      None
    } catch {
      case e: Exception => throw(new FacebookFailureException(exc = Option(e)))
    }
    friendsToMap(friends)
  }
}

object MaidenFacebook {

  def createTestUser() = {
    val app_id = MaidenConfig.get[String]("social.fb.app_id")
    val accessToken = createAppAccessToken
    val uri = s"https://graph.facebook.com/${app_id}/accounts/test-users"
    val data = Map(
      "access_token" -> Seq(accessToken)
    )

    val c = new HttpClient(uri, method="POST", data = data)
    val response = c.fetchAsMap
    response("access_token").toString
  }

  def createAppAccessToken() = {
    val app_id = MaidenConfig.get[String]("social.fb.app_id")
    val app_secret = MaidenConfig.get[String]("social.fb.api_secret")
    val uri = s"https://graph.facebook.com/oauth/access_token"
    val data = Map(
      "client_id" -> Seq(app_id),
      "client_secret" -> Seq(app_secret),
      "grant_type" -> Seq("client_credentials")
    )

    val c = new HttpClient(uri, method="POST", data = data)
    val response = c.fetch
    response.split('=')(1)
  }

}
