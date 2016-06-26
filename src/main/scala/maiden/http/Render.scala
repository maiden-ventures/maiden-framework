package maiden.http

import scala.util.{Try, Success, Failure}
import io.finch.{Endpoint, _}
import maiden.types._
import maiden.types.Exceptions._
/**
  * Created by glen on 6/20/16.
  */
object Render {

  def render2[E <: MaidenException, T](body:  => Either[E, T]) = {
    body match {
      case Right(b) => Ok(b)
      //
      case Left(e) => InternalServerError(new Exception(e.message))
    }
    //} handle {
    //  case e: Exception => { log.errorST("Render Exception", e); BadRequest(e) }
  }

  def render[T](body: => T) = {
    val result: Either[MaidenException, T] = Try (body) match {
      case Success(x) => Right(x)
      //TODO:  handle speciality cases here
      case Failure(e) => { println(e); Left(MaidenRenderException) }
    }
    result match {
      case Right(b) => Ok(b)
      //TODO: pattern match on exception so we return the right status code
      case Left(e) =>  InternalServerError(new Exception(e.message))
    }
  }
}
