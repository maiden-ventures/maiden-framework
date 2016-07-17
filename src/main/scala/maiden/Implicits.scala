package maiden.implicits

import java.time.{Instant, LocalDate, LocalDateTime, ZoneId}
import java.util.Date
import java.util.concurrent.Executors
import io.getquill._
import scala.concurrent.ExecutionContext

object DateImplicits {
  private[this] def dateToLocalDate(date: Date) =
    Instant.ofEpochMilli(date.getTime).atZone(ZoneId.systemDefault).toLocalDate

  //conversions between LocalDateTime and java Date
  implicit val decodeLocalDateTime = mappedEncoding[Date, LocalDateTime](date =>
    LocalDateTime.ofInstant(date.toInstant, ZoneId.systemDefault()))

  implicit val encodeLocalDateTime = mappedEncoding[LocalDateTime, Date](time =>
    Date.from(time.atZone(ZoneId.systemDefault).toInstant))

  implicit val decodeLocalDate = mappedEncoding[Date, LocalDate](d =>  dateToLocalDate(d))

  implicit val encodeDateTime = mappedEncoding[LocalDate, Date](localDate =>
    Date.from(localDate.atStartOfDay(ZoneId.systemDefault).toInstant))


  implicit val encodeOptionDateTime = mappedEncoding[Option[LocalDate], Option[Date]](localDate =>
    localDate match {
      case Some(s) => Option(Date.from(s.atStartOfDay(ZoneId.systemDefault).toInstant))
      case _ => None
    })

  implicit val decodeOptionLocalDate = mappedEncoding[Option[Date], Option[LocalDate]](d =>
    d match {
      case Some(x) => Option(dateToLocalDate(x))
      case _ => None
    })

  implicit val encodeOptionLocalDateTime = mappedEncoding[Option[LocalDateTime], Option[Date]](localDate =>
    localDate match {
      case Some(s) =>
        Option(Date.from(s.atZone(ZoneId.systemDefault).toInstant))
      case _ => None
    })

  implicit val decodeOptionLocalDateTime = mappedEncoding[Option[Date], Option[LocalDateTime]](date =>
    date match {
      case Some(x) =>
        Option(LocalDateTime.ofInstant(x.toInstant, ZoneId.systemDefault()))
      case _ => None
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
