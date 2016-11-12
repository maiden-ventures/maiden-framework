package maiden.implicits


import java.time._
import scala.language.implicitConversions
import scala.reflect.ClassTag
import java.sql.{PreparedStatement, ResultSet, Types}
//import io.getquill.context.BindedStatementBuilder
import io.getquill._

trait DBImplicits {
  this: JdbcContext[_,_] =>

  implicit def dt2optdt(v: LocalDateTime): Option[LocalDateTime] = Option(v)
  implicit def str2optstr(v: String): Option[String] = Option(v)
  implicit def long2optlong(v: Long): Option[Long] = Option(v)
  implicit def bd2optbd(v: BigDecimal): Option[BigDecimal] =  Option(v)
  implicit def int2optint(v: Int): Option[Int] = Option(v)

  val SqlNull = quote(infix"null")

  implicit class ForUpdate[T](q: Query[T]) {
    def forUpdate = quote(infix"$q FOR UPDATE".as[Query[T]])
  }

  //quill uses java.math.BigDecimal for some reason
  implicit def scalaBigDecimalToJavaBigDecimal(bd: scala.math.BigDecimal): java.math.BigDecimal =
    bd.bigDecimal

  implicit def javaBigDecimalToScalaBigDecimal(bd: java.math.BigDecimal): scala.math.BigDecimal =
    scala.math.BigDecimal(bd)

  //private[this] val nullEncoder = encoder[Int](_.setNull)
  implicit class StringOps(f: String) {
    def lt (right: String) = quote(infix"$f < $right".as[Boolean])
    def gt (right: String) = quote(infix"$f > $right".as[Boolean])
    def lte (right: String) = quote(infix"$f <= $right".as[Boolean])
    def gte (right: String) = quote(infix"$f >= $right".as[Boolean])
  }

  implicit class OptionStringOps(f: Option[String]) {
    def lt (right: String) = quote(infix"$f < $right".as[Boolean])
    def gt (right: String) = quote(infix"$f > $right".as[Boolean])
    def lte (right: String) = quote(infix"$f <= $right".as[Boolean])
    def gte (right: String) = quote(infix"$f >= $right".as[Boolean])
  }

  implicit class OptionOps[T](f: Option[T]) {
    def < (right: T) = quote(infix"$f < $right".as[Boolean])
    def > (right: T) = quote(infix"$f > $right".as[Boolean])
    def <= (right: T) = quote(infix"$f <= $right".as[Boolean])
    def >= (right: T) = quote(infix"$f >= $right".as[Boolean])
  }

  implicit class DateTimeOps(f: LocalDateTime) {
    def < (right: LocalDateTime) = quote(infix"$f < $right".as[Boolean])
    def > (right: LocalDateTime) = quote(infix"$f > $right".as[Boolean])
    def <= (right: LocalDateTime) = quote(infix"$f <= $right".as[Boolean])
    def >= (right: LocalDateTime) = quote(infix"$f >= $right".as[Boolean])
  }


  implicit class IsNull(f: Option[_])  {
    def isNull = quote(infix"$f is null".as[Boolean])
    def notNull = quote(infix"$f is not null".as[Boolean])
  }

  implicit class Limit[T](q: Query[T]) {
    def limit = quote { (offset: Int, size: Int) =>
      infix"$q limit $offset, $size".as[Query[T]]
    }
  }

  implicit class OptionBetween[T](f: Option[T]) {
    def between(start: T, end: T) =
      quote {
        (start: T, end: T) =>
          f >= start && f <= end
          //infix"$f between $start and $end".as[Boolean]
      }
  }

  implicit class Between[T: ClassTag](f: T) extends OptionBetween(Option(f))
  implicit class DateBetween(f: LocalDateTime)
      extends OptionBetween[LocalDateTime](Option(f))
}
