package maiden.api.v1.hello

import scala.language.implicitConversions
import maiden.types.Exceptions._
import maiden.types.MaidenApi
import maiden.http.RequiredAuthRequestReader._
import maiden.auth.AuthenticatedClient
import maiden.http.Render.{render}
import io.finch.{Endpoint, _}



sealed trait ResponseType

case class Hello(id: Long,  name: String) extends ResponseType


object HelloApi extends MaidenApi {

  def test(name: String): Either[MaidenException, Hello] = {
    if (name == "glen") {
      Right(Hello(0l, name))
    } else {
      Left(MaidenModelNotFoundException)
    }
  }

  def getH(h: Hello): Either[MaidenException, Hello] = Right(h)

  def helloApi() = createHello //:+: dumbHello //getHello :+: getHello2

  val prefix = "v1" :: "hello"

  /*def getHello: Endpoint[Hello] =
    get(prefix :: string("name")) { (name: String) =>

      respondBuiltIn(test(name))
    }

   */

  val hello: Endpoint[Hello] = (param("id").as[Long] :: param("name").as[String]).as[Hello]

  def createHello: Endpoint[Hello] =
    get(prefix :: "create" :: hello :: authorize) { (h: Hello, c: AuthenticatedClient) => {
      println(c)
      render(getH(h))
    }}

  /*def dumbHello: Endpoint[String] =
    get(prefix) {
      render("HELLO!")
    }

   */

  /*
  def getHello2: Endpoint[Hello] = respond {
    get(prefix :: "get" :: long("id")) { (id: Long) =>
      test(id) match {
        case Right(id) => Ok(Hello(id.toString))
        case Left(e) => NotFound(e)
      }
    }
  }

  def getHello: Endpoint[Hello] = respond {
    post(prefix :: "get" :: long("id") ) { (id: Long) =>
      Ok(Hello(s"My id is ${id}"))
    } handle {
      case e: Exception => BadRequest(e)
    }
  }
   */

}
