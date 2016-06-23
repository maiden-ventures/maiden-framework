package maiden.types

import Exceptions._

/**
  * Created by glen on 6/20/16.
  */
object Implicits {

  implicit def any2either[E <: MaidenException, T <: ResponseType](v: T): Either[E,T] = Right(v)

}
