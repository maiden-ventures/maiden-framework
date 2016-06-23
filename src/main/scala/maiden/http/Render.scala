package maiden.http

import io.finch.{Endpoint, _}
import maiden.types._
import maiden.types.Exceptions._
/**
  * Created by glen on 6/20/16.
  */
object Render {

  def render[E <: MaidenException, T](body:  => Either[E, T]) = {
    body match {
      case Right(b) => Ok(b)
      //
      case Left(e) => InternalServerError(new Exception(e.message))
    }
    //} handle {
    //  case e: Exception => { log.errorST("Render Exception", e); BadRequest(e) }
  }

}
