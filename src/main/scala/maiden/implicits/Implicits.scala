package maiden.implicits

import java.time.{Instant, LocalDate, LocalDateTime, ZoneId, ZonedDateTime, ZoneOffset}
import java.util.Date
import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._

object JsonImplicits {


  implicit val dateTimeEncoder: Encoder[Date] =
    Encoder.instance(a => a.getTime.asJson)

  implicit val optionDateTimeEncoder: Encoder[Option[Date]] =
    Encoder.instance(a => (new Date().asJson))

    implicit val dateTimeDecoder: Decoder[Date] =
      Decoder.instance(a => a.as[Long]
                         .map(new Date(_)))

}


object ExecutionImplicits {

  implicit val ec = new ExecutionContext {
    val threadPool = Executors.newFixedThreadPool(10);
    override def reportFailure(cause: Throwable): Unit = {};
    override def execute(runnable: Runnable): Unit = threadPool.submit(runnable);
    def shutdown() = threadPool.shutdown();
  }

}
