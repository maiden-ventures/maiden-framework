package maiden.http

import java.util.concurrent.Executors
import scala.concurrent.Future
import java.io.{StringWriter, PrintWriter}
import scala.util.{Try, Success, Failure}
import scala.collection.JavaConverters._
import com.twitter.util.FuturePool
import io.netty.handler.codec.http.{HttpResponseStatus => H}
import io.finch._
import maiden.exceptions._
import maiden.implicits.ExecutionImplicits._

object Render {

  private[this] val futurePool = FuturePool(Executors.newCachedThreadPool())

  def render[T](body: => T) = {
    //Future {
      val result: Either[MaidenException, T] = Try (body) match {
        case Success(x) => Right(x)
        //TODO:  handle speciality cases here
        case Failure(e) if e.isInstanceOf[MaidenException] => Left(e.asInstanceOf[MaidenException])
        case Failure(e) => {
          val sw = new StringWriter
          e.printStackTrace(new PrintWriter(sw))
          Left(new MaidenException(sw.toString))
        }
      }
      result match {
        case Right(b) => Ok(b)
        //TODO: pattern match on exception so we return the right status code
        case Left(e) => {
          e.httpStatus match {
            case H.BAD_REQUEST => BadRequest(e)
            case H.NOT_ACCEPTABLE => BadRequest(e)
            case H.NOT_FOUND => NotFound(e)
            case _ => InternalServerError(e)

          }
        }
      }
    //}
  }

}
