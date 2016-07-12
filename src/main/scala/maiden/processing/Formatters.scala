package maiden.processing

import com.google.i18n.phonenumbers._
import io.circe._
import io.circe.parser._

import cats.data.Xor

//various formatters for various types
object Formatters {

  val phoneUtil = PhoneNumberUtil.getInstance

  implicit class StringFormatters(value: String) {

    def phone(country: String = "US") = {
      val parsed = phoneUtil.parse(value, country)
      phoneUtil.formatInOriginalFormat(parsed, country)
    }

    def phone: String = phone("US")

    def postal_code(country: String) = {
      value
    }

    def postal_code: String = postal_code("US")

    def list(sep: String) = value.split(sep).map(_.trim).mkString(",")

    def list: String = list(",")

    def asList(sep: String) = list.split(sep).toList

    def asList: List[String] = asList(",")

    def asJson = parse(value).getOrElse(Json.Null)
  }

}
