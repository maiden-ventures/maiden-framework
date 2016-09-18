package maiden.implicits

import java.time._
import java.time.{LocalDateTime, ZoneId, ZoneOffset}
import java.util.Date
import io.getquill._
import io.circe.java8._
import maiden.date_formatters.DateFormatters._

trait  DateImplicits {

  this: JdbcContext[_,_] =>

  private[this] def date2ldt(d: Date) =
    LocalDateTime.ofInstant(d.toInstant, ZoneId.systemDefault())

  private[this] def ldt2date(ldt: LocalDateTime) =
    Date.from(ldt.atZone(ZoneId.systemDefault).toInstant)

  implicit val decodeLocalDateTime = MappedEncoding[Date, LocalDateTime](date => date2ldt(date))

  implicit val encodeLocalDateTime = MappedEncoding[LocalDateTime, Date](ldt => ldt2date(ldt))

  //handle ordering of datetimes
  implicit val localDateTimeOrder: Ordering[LocalDateTime] = null
  implicit val localDateOrder: Ordering[LocalDate] = null

}
