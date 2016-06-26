package maiden.implicits

import java.time.{ LocalDateTime, ZoneId, ZoneOffset }
import java.util.{Date}
import io.getquill._
import io.getquill.quotation._
import io.getquill.naming.SnakeCase
import io.getquill.sources.sql.ops._
import cats.data.Xor, io.circe._, io.circe.jawn._
import io.circe.java8._
import scala.concurrent.ExecutionContext.Implicits.global



object DateImplicits {
  //implicit conversions to/from joda
  implicit val decodeLocalTime = mappedEncoding[Date, LocalDateTime](date => LocalDateTime.ofInstant(date.toInstant, ZoneId.systemDefault()))
  implicit val encodeLocalTime = mappedEncoding[LocalDateTime, Date](time => new Date(time.toEpochSecond(ZoneOffset.of(ZoneId.systemDefault().getId))))

  /*
   val jodaTzDateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSSSSSZ")
   implicit val jodaDateTimeEncoder = db.encoder[DateTime](
   (d: DateTime) => d.toString(jodaTzDateTimeFormatter)
   )
   implicit val jodaDateTimeDecoder = db.decoder[DateTime] {
   case s: String => DateTime.parse(s, jodaTzDateTimeFormatter)
   }
   */


  implicit val timestampOrd: Ordering[LocalDateTime] = null

  implicit class RichDateTime(a: LocalDateTime) {
    def >=(b: LocalDateTime) = quote(infix"$a >= $b".as[Boolean])

    def <=(b: LocalDateTime) = quote(infix"$a <= $b".as[Boolean])

    def >(b: LocalDateTime) = quote(infix"$a > $b".as[Boolean])
    def <(b: LocalDateTime) = quote(infix"$a < $b".as[Boolean])
  }

  implicit class ForUpdate[T](q: Query[T]) {
    def forUpdate = quote(infix"$q FOR UPDATE".as[Query[T]])
  }

}
