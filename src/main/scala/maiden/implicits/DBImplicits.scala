package maiden.implicits


import java.time._
import scala.reflect.ClassTag
import java.sql.{PreparedStatement, ResultSet, Types}
import io.getquill.context.BindedStatementBuilder
import io.getquill.JdbcContext

trait DBImplicits {
  this: JdbcContext[_,_] =>

  val SqlNull = quote(infix"null")

  implicit class ForUpdate[T](q: Query[T]) {
    def forUpdate = quote(infix"$q FOR UPDATE".as[Query[T]])
  }

  implicit val localDateTimeDecoder: Decoder[LocalDateTime] =
    new Decoder[LocalDateTime] {
      def apply(index: Int, row: ResultSet) = {
        val v = row.getTimestamp(index + 1)
        if (v == null)
          LocalDateTime.MIN
        else {
          LocalDateTime.ofInstant(v.toInstant,
                                  ZoneId.systemDefault())
        }
      }
    }

  private[this] val nullEncoder = encoder[Int](_.setNull)

  override implicit def optionEncoder[T](implicit d: Encoder[T]): Encoder[Option[T]] =
    new Encoder[Option[T]] {
      override def apply(idx: Int, value: Option[T], row: BindedStatementBuilder[PreparedStatement]) =
        value match {
          case Some(value) => d(idx, value, row)
          case None =>
            import Types._
            val sqlType =
              d match {
                case `stringEncoder`     => VARCHAR
                case `bigDecimalEncoder` => NUMERIC
                case `booleanEncoder`    => BOOLEAN
                case `byteEncoder`       => TINYINT
                case `shortEncoder`      => SMALLINT
                case `intEncoder`        => INTEGER
                case `longEncoder`       => BIGINT
                case `floatEncoder`      => REAL
                case `doubleEncoder`     => DOUBLE
                case `byteArrayEncoder`  => VARBINARY
                case `dateEncoder`       => TIMESTAMP
                case _                   => INTEGER
              }
            nullEncoder(idx, sqlType, row)
        }
    }

  //quill uses java.math.BigDecimal for some reason
  implicit def scalaBigDecimalToJavaBigDecimal(bd: scala.math.BigDecimal): java.math.BigDecimal =
    bd.bigDecimal

  implicit def javaBigDecimalToScalaBigDecimal(bd: java.math.BigDecimal): scala.math.BigDecimal =
    scala.math.BigDecimal(bd)

  //private[this] val nullEncoder = encoder[Int](_.setNull)


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


  /*
  def between[T] = quote {
    new {
      def apply[T](start: T, end: T, value: T) =
        if (start > value && end < value) true
        else false
    }
  }

  val between = quote {
    new {
      def apply[T](xs: Query[T])(p: T => Boolean) =
        xs.filter(p(_)).nonEmpty
    }
}
   */

}
