package maiden.date_formatters

import java.text.SimpleDateFormat
import java.time.temporal.ChronoField
import java.time.{LocalDate, ZonedDateTime, LocalDateTime}
import java.time.format.{DateTimeFormatterBuilder, DateTimeFormatter}
import java.util.Date

object DateFormatters {
  private val datePattern = "yyyy-MM-dd HH:mm:ss"
  private val df = new SimpleDateFormat(datePattern)

  val dateTimeFormat = DateTimeFormatter.ofPattern(datePattern)

  val zonedDateTimeFormatter =
    new DateTimeFormatterBuilder()
      .append(DateTimeFormatter.ofPattern(datePattern))
      .optionalStart()
      .appendFraction(ChronoField.NANO_OF_SECOND, 0, 6, true)
      .optionalEnd()
      .appendOffset("+HH:mm", "+00")
      .toFormatter()

  def parseDate(strDate: String): Date = df.parse(strDate)

  def formatDate(date: Date): String = df.format(date)

  def parseLocalDateTime(strDate: String): LocalDateTime = LocalDateTime.parse(strDate, dateTimeFormat)

  def formatLocalDateTime(dateTime: LocalDateTime): String = dateTime.format(dateTimeFormat)

  def parseZonedDateTime(strDate: String): ZonedDateTime = ZonedDateTime.parse(strDate, zonedDateTimeFormatter)

  def formatZonedDateTime(dateTime: ZonedDateTime): String = dateTime.format(zonedDateTimeFormatter)

  def parseLocalDate(strDate: String): LocalDate = LocalDate.parse(strDate, DateTimeFormatter.ISO_LOCAL_DATE)

  def formatLocalDate(dateTime: LocalDate): String = dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE)
}
