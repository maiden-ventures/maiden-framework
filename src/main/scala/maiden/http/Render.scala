package maiden.http

import java.io.{StringWriter, PrintWriter}
import scala.util.{Try, Success, Failure}
import io.netty.handler.codec.http.{HttpResponseStatus => H}
import io.finch._
import maiden.exceptions._

object Render {

  def render[T](body: => T) = {
    val result: Either[MaidenException, T] = Try (body) match {
      case Success(x) => Right(x)
      //TODO:  handle speciality cases here
      case Failure(e) if e.isInstanceOf[MaidenException] => Left(e.asInstanceOf[MaidenException])
      case Failure(e) => {
        val sw = new StringWriter
        e.printStackTrace(new PrintWriter(sw))
        println(sw.toString)
        Left(new MaidenException("unhandled"))
      }
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
