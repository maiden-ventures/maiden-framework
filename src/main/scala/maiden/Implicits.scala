package maiden.implicits

import java.sql.ResultSet
import java.time.{Instant, LocalDate, LocalDateTime, ZoneId, ZonedDateTime, ZoneOffset}
import java.util.Date
import java.util.concurrent.Executors
import io.getquill.context._
import io.circe.java8._
import scala.concurrent.ExecutionContext
import maiden.models.DB._
import maiden.date_formatters.DateFormatters._


object JsonImplicits {

  import io.circe._
  import io.circe.generic.auto._
  import io.circe.syntax._


  implicit val dateTimeEncoder: Encoder[Date] = {
    Encoder.instance(a => {
                       println("encoder")
                       println(a)
                       a.getTime.asJson
                     })
    }

  implicit val optionDateTimeEncoder: Encoder[Option[Date]] = {
    Encoder.instance(a => (new Date().asJson))
   }

    implicit val dateTimeDecoder: Decoder[Date] = {
      Decoder.instance(a => {
                         println("decoder")
                         println(a)
                         a.as[Long].map(new Date(_))
                      })
      }


}
object DateImplicits {
  import db._


  implicit val decodeLocalTime = mappedEncoding[Date, LocalDateTime](date => LocalDateTime.ofInstant(date.toInstant, ZoneId.systemDefault()))
  implicit val encodeLocalTime = mappedEncoding[LocalDateTime, Date](time => new Date(time.toEpochSecond(ZoneOffset.of(ZoneId.systemDefault().getId))))

  implicit val optionLocalDateTimeDecoder: Decoder[Option[LocalDateTime]] =
    decoder[Option[LocalDateTime]] {
      row => index =>
      row.getObject(index) match {
        case c: Date if c != null => Option(LocalDateTime.parse(c.toString.replace(" ", "T")))
        case _ => None
      }
    }


  implicit val localDateTimeDecoder: Decoder[LocalDateTime] =
    decoder[LocalDateTime] {
      row => index =>
      row.getObject(index) match {
        case c: String if c != null => LocalDateTime.parse(c.replace(" ", "T"))
        case _ => LocalDateTime.now
      }
    }

  implicit val localDateTimeEncoder: Encoder[LocalDateTime] =
    encoder[LocalDateTime] {
      row => (idx, ldt) =>
      row.setObject(idx, localDateToDate(ldt), java.sql.Types.DATE)          //          //.OTHER) // database-specific implementation
    }

  private[this] def dateToLocalDate(date: Date) =
    Instant.ofEpochMilli(date.getTime).atZone(ZoneId.systemDefault).toLocalDate

  private[this] def localDateToDate(ld: LocalDateTime) =
    Date.from(ld.atZone(ZoneId.systemDefault).toInstant)

  //conversions between LocalDateTime and java Date
  implicit val decodeLocalDateTime = mappedEncoding[Date, LocalDateTime](date =>
    LocalDateTime.ofInstant(date.toInstant, ZoneId.systemDefault()))

  implicit val encodeLocalDateTime = mappedEncoding[LocalDateTime, Date](time =>
    Date.from(time.atZone(ZoneId.systemDefault).toInstant))

  implicit val decodeLocalDate = mappedEncoding[Date, LocalDate](d =>  dateToLocalDate(d))

  implicit val encodeDateTime = mappedEncoding[LocalDate, Date](localDate =>
    Date.from(localDate.atStartOfDay(ZoneId.systemDefault).toInstant))


  implicit val encodeOptionDateTime = mappedEncoding[Option[LocalDate], Date](localDate =>
    localDate match {
      case Some(s) => Date.from(s.atStartOfDay(ZoneId.systemDefault).toInstant)
      case _ => new Date(0)
    })

  implicit val decodeOptionLocalDate = mappedEncoding[Date, Option[LocalDate]](d =>
    d match {
      case x: Date => Option(dateToLocalDate(x))
      case _ => {
        val d = new Date(0)
        Option(Instant.ofEpochMilli(d.getTime()).atZone(ZoneId.systemDefault).toLocalDate())

      }
    })

  implicit val encodeOptionLocalDateTime = mappedEncoding[Option[LocalDateTime], Option[Date]](localDate =>
    localDate match {
      case Some(s) =>
        Option(Date.from(s.atZone(ZoneId.systemDefault).toInstant))
      case _ => {
        val d = new Date(0)
        Option(d)

      }
    })

  implicit val encodeOptionLocalDateTimeToDate = mappedEncoding[Option[LocalDateTime], Date](localDate =>
    localDate match {
      case Some(s) =>
        Date.from(s.atZone(ZoneId.systemDefault).toInstant)
      case _ => {
        val d = new Date(0)
        d
      }
    })
  implicit val decodeOptionLocalDateTime = mappedEncoding[Option[Date], Option[LocalDateTime]](date =>
    date match {
      case Some(x) =>
        Option(LocalDateTime.ofInstant(x.toInstant, ZoneId.systemDefault()))
      case _ => {
        val d = new Date(0)
        Option(LocalDateTime.ofInstant(d.toInstant, ZoneId.systemDefault()))
      }
    })


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

  implicit class RichLocalDate(a: LocalDate) {
    def >(b: LocalDate) = quote(infix"$a > $b".as[Boolean])
    def >=(b: LocalDate) = quote(infix"$a >= $b".as[Boolean])
    def <(b: LocalDate) = quote(infix"$a < $b".as[Boolean])
    def <=(b: LocalDate) = quote(infix"$a <= $b".as[Boolean])
  }



}

object DBImplicits {

  import db._
  val SqlNull = quote(infix"null")

  implicit class ForUpdate[T](q: Query[T]) {
    def forUpdate = quote(infix"$q FOR UPDATE".as[Query[T]])
  }

  implicit class ReturningId[T](a: Action[T]) {
    def returningId = quote(infix"$a RETURNING ID".as[Query[T]])
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
