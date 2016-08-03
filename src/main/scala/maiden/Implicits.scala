package maiden.implicits

import java.sql.ResultSet
import java.time.{Instant, LocalDate, LocalDateTime, ZoneId, ZonedDateTime, ZoneOffset}
import java.util.Date
import java.util.concurrent.Executors
import io.getquill.context._
import io.circe.java8._
import scala.concurrent.ExecutionContext
import maiden.date_formatters.DateFormatters._
import io.getquill.JdbcContext

//can go away when we no longer have to override Quill's Option Encoder
import java.sql
import java.sql.PreparedStatement
import java.sql.Types
import java.util
import java.util.Calendar
import java.util.TimeZone

import io.getquill.context.BindedStatementBuilder
import io.getquill.JdbcContext



object JsonImplicits {

  import io.circe._
  import io.circe.generic.auto._
  import io.circe.syntax._


  implicit val dateTimeEncoder: Encoder[Date] =
    Encoder.instance(a => a.getTime.asJson)

  implicit val optionDateTimeEncoder: Encoder[Option[Date]] =
    Encoder.instance(a => (new Date().asJson))

    implicit val dateTimeDecoder: Decoder[Date] =
      Decoder.instance(a => a.as[Long].map(new Date(_)))

}

trait  DateImplicits {

  this: JdbcContext[_,_] =>

  private[this] def date2ldt(d: Date) =
    LocalDateTime.ofInstant(d.toInstant, ZoneId.systemDefault())

  private[this] def ldt2date(ldt: LocalDateTime) =
    Date.from(ldt.atZone(ZoneId.systemDefault).toInstant)

  implicit val decodeLocalDateTime = mappedEncoding[Date, LocalDateTime](date => date2ldt(date))

  implicit val encodeLocalDateTime = mappedEncoding[LocalDateTime, Date](ldt => ldt2date(ldt))

  //handle ordering of datetimes
  implicit val localDateTimeOrder: Ordering[LocalDateTime] = null
  implicit val localDateOrder: Ordering[LocalDate] = null

  //for date queries
  implicit class RichLocalDateTime(a: LocalDateTime) {
    def >(b: LocalDateTime) = quote(infix"$a > $b".as[Boolean])
    def >=(b: LocalDateTime) = quote(infix"$a >= $b".as[Boolean])
    def <(b: LocalDateTime) = quote(infix"$a < $b".as[Boolean])
    def <=(b: LocalDateTime) = quote(infix"$a <= $b".as[Boolean])
  }

  implicit class RichLocalDateTimeOption(a: Option[LocalDateTime]) extends RichLocalDateTime(a.getOrElse(null))



  implicit class RichLocalDate(a: LocalDate) {
    def >(b: LocalDate) = quote(infix"$a > $b".as[Boolean])
    def >=(b: LocalDate) = quote(infix"$a >= $b".as[Boolean])
    def < (b: LocalDate) = quote(infix"$a < $b".as[Boolean])
    def <=(b: LocalDate) = quote(infix"$a <= $b".as[Boolean])
  }

  implicit class RichLocalDateOption(a: Option[LocalDate]) extends RichLocalDate(a.getOrElse(null))
}

trait DBImplicits {
  this: JdbcContext[_,_] =>

  val SqlNull = quote(infix"null")

  implicit class ForUpdate[T](q: Query[T]) {
    def forUpdate = quote(infix"$q FOR UPDATE".as[Query[T]])
  }

  //this is an override for quill until it supports inserting Option[LocalDateTime]

  private[this] val nullEncoder = encoder[Int](_.setNull)

  override implicit def optionEncoder[T](implicit d: Encoder[T]): Encoder[Option[T]] =
    new Encoder[Option[T]] {
      override def apply(idx: Int, value: Option[T], row: BindedStatementBuilder[PreparedStatement]) =
        value match {
          case Some(value) => d(idx, value, row)
          case None => {println(d);
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
                case _                   => TIMESTAMP
              }
            nullEncoder(idx, sqlType, row)
        }}
    }
}

object ExecutionImplicits {

  implicit val ec = new ExecutionContext {
    val threadPool = Executors.newFixedThreadPool(10);
    override def reportFailure(cause: Throwable): Unit = {};
    override def execute(runnable: Runnable): Unit = threadPool.submit(runnable);
    def shutdown() = threadPool.shutdown();
  }

}
