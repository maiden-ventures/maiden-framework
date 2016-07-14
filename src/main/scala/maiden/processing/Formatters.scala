package maiden.processing

import scala.language.implicitConversions
import com.google.i18n.phonenumbers._
import io.circe._
import io.circe.parser._

//various formatters for various types
object Formatters {

  val phoneUtil = PhoneNumberUtil.getInstance

  abstract class StringFormatterOps(value: String) {

    def phone(country: String = "US"): String = {
      val parsed = phoneUtil.parse(value, country)
      phoneUtil.formatInOriginalFormat(parsed, country)
    }

    def phone: String = phone("US")

    def postal_code(country: String) = {
      if (value.size > 5) {
        //format at 98717-1641
        val s = value.replace("-", "")
        "%s-%s".format(
          s.substring(0,5),
          s.substring(5,9)
        )

      } else {
        //format as 98717-0000
        s"${value}-0000"
      }
    }

    def postal_code: String = postal_code("US")

    //TODO: error checking and convert to regex
    def social_security = {
      val s = value.replace("-", "")
      "%s-%s-%s".format(
        s.substring(0,3),
        s.substring(3,5),
        s.substring(5,9)
      )
    }

    def list(sep: String) = value.split(sep).map(_.trim).mkString(",")

    def list: String = list(",")
    def asList(sep: String) = list.split(sep).toList

    def asList: List[String] = asList(",")

    def asJson = parse(value).getOrElse(Json.Null)
  }

  implicit class ImplicitStringFormatterOps(s: String) extends StringFormatterOps(s)

  //TODO: FIX THIS... it more than likely does not do what we want
  implicit class ImplicitOptionStringFormatterOps(s: Option[String]) {
    val v = s.getOrElse("")
    def phone(country: String = "US") = {
      val parsed = phoneUtil.parse(v, country)
      Option(phoneUtil.formatInOriginalFormat(parsed, country))
    }

    def phone: Option[String] = phone("US")

    def postal_code(country: String) = {
      //hardcoded to US for now
      val zip = if (v.size > 5) {
        //format at 98717-1641
        val v2 = v.replace("-", "")
        "%s-%s".format(
          v2.substring(0,5),
          v2.substring(5,9)
        )

      } else {
        //format as 98717-0000
        s"${v}-0000"
      }
      Option(zip)
    }

    def postal_code: Option[String] = postal_code("US")

    //TODO: error checking and convert to regex
    def social_security = {
      v.replace("-", "")
      "%s-%s-%s".format(
        v.substring(0,3),
        v.substring(3,5),
        v.substring(5,9)
      )
    }

    def list(sep: String) = Option(v.split(sep).map(_.trim).mkString(","))

    def list: Option[String] = list(",")
    def asList(sep: String) = list.getOrElse(",").split(sep).toList

    def asList: List[String] = asList(",")

    def asJson = parse(v).getOrElse(Json.Null)
  }

}
