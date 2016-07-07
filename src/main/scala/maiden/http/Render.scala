package maiden.http

import scala.util.{Try, Success, Failure}
import io.netty.handler.codec.http.{HttpResponseStatus => H}
import io.finch.{Endpoint, _}
import maiden.types._
import maiden.exceptions._

object Render {

  def render[T](body: => T) = {
    val result: Either[MaidenException, T] = Try (body) match {
      case Success(x) => Right(x)
      //TODO:  handle speciality cases here
      case Failure(e) if e.isInstanceOf[MaidenException] => Left(e.asInstanceOf[MaidenException])
      case _ => Left(new MaidenException("unhandled"))
    }
    result match {
      case Right(b) => Ok(b)
      //TODO: pattern match on exception so we return the right status code
      case Left(e) => {
        e.httpStatus match {
          case H.BAD_REQUEST => InternalServerError(e)
          case H.NOT_FOUND => NotFound(e)
          case _ => InternalServerError(e)

        }
      }
    }
  }

}
