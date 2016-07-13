package maiden.processing

import scala.language.implicitConversions
import com.google.i18n.phonenumbers._
import io.circe._
import io.circe.parser._

//various formatters for various types
object Formatters {

  implicit def optionstring2string(s: Option[String]): String = s match {
    case Some(x) => x
    case _ => ""
  }

  val phoneUtil = PhoneNumberUtil.getInstance

  abstract class StringFormatterOps(value: String) {
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

  implicit class ImplicitStringFormatterOps(s: String) extends StringFormatterOps(s)

  //TODO: FIX THIS... it more than likely does not do what we want
  implicit class ImplicitOptionStringFormatterOps(s: Option[String])
      extends StringFormatterOps(s.getOrElse(""))

  }
