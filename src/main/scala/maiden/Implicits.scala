package maiden.implicits

import java.time.{ LocalDateTime, ZoneId, ZoneOffset }
import java.util.{Date}
import java.util.concurrent.Executors
import io.getquill._
//import io.getquill.quotation._
//import io.getquill.naming.SnakeCase
//import io.getquill.sources.sql.ops._
//import cats.data.Xor, io.circe._, io.circe.jawn._
//import io.circe.java8._
import scala.concurrent.ExecutionContext



object DateImplicits {

  //conversions between LocalDateTime and java Date
  implicit val decodeLocalTime = mappedEncoding[Date, LocalDateTime](date => LocalDateTime.ofInstant(date.toInstant, ZoneId.systemDefault()))
  implicit val encodeLocalTime = mappedEncoding[LocalDateTime, Date](time => Date.from(time.atZone(ZoneId.systemDefault).toInstant))


  //handle ordering of datetimes
  implicit val timestampOrd: Ordering[LocalDateTime] = null

  //for date queries
  implicit class RichDateTime(a: LocalDateTime) {
    def >=(b: LocalDateTime) = quote(infix"$a >= $b".as[Boolean])

    def <=(b: LocalDateTime) = quote(infix"$a <= $b".as[Boolean])

    def >(b: LocalDateTime) = quote(infix"$a > $b".as[Boolean])
    def <(b: LocalDateTime) = quote(infix"$a < $b".as[Boolean])
  }

}

object DBImplicits {

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
