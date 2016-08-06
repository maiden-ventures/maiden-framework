package maiden.processing

import java.math.RoundingMode
import java.time.LocalDateTime
import java.net.URL
import scala.util.matching._
import com.twitter.util.{Future, Return, Throw, Try}
import io.finch.internal._
import spire.math._
import spire.implicits._
import io.finch._
import com.google.i18n.phonenumbers._
import io.circe.parser._

object Validations {



/* allow for *some* raw values to behave like Finch endpoints
   with regard to validations
*/

  abstract class EndpointLikeOps[T](value: T) {
    def should(name: String)(rule: ValidationRule[T]) =
      if (rule.apply(value)) Future.value(value)
      else Future.exception(Error(s" ${name} should ${rule.description}"))

    def should(rule: ValidationRule[T]): Future[T] = should(s""""${value}"""")(rule)
  }

  abstract class EndpointOptionLikeOps[T](value: Option[T]) {

    def should(name: String)(rule: ValidationRule[T]) =
      if (rule.apply(value)) Future.value(value)
      else Future.exception(Error(s" ${name} should ${rule.description}"))

  }

  //class EndpointLikeString(s: String) extends EndpointLikeOps[String](s)
  implicit class EndpointLikeInt(i: Int) extends EndpointLikeOps[Int](i)
  implicit class EndpointLikeLong(l: Long) extends EndpointLikeOps[Long](l)
  implicit class EndpointLikeDouble(d: Double) extends EndpointLikeOps[Double](d)
  implicit class EndpointLikeFloat(f: Float) extends EndpointLikeOps[Float](f)
  implicit class EndpointLikeLocalDateTime(d: LocalDateTime) extends EndpointLikeOps[LocalDateTime](d)
  implicit class EndpointLikeBigDecimal(bd: BigDecimal) extends EndpointLikeOps[BigDecimal](bd)


  implicit class EndpointLikeOptionString(o: Option[String])
      extends EndpointOptionLikeOps[String](o)

  implicit class EndpointLikeOptionInt(o: Option[Int])
      extends EndpointOptionLikeOps[Int](o)

  implicit class EndpointLikeOptionLong(o: Option[Long])
      extends EndpointOptionLikeOps[Long](o)

  implicit class EndpointLikeOptionDouble(o: Option[Double])
      extends EndpointOptionLikeOps[Double](o)

  implicit class EndpointLikeOptionFloat(o: Option[Float])
      extends EndpointOptionLikeOps[Float](o)

  implicit class EndpointLikeOLocalDateTime(o: Option[LocalDateTime])
      extends EndpointOptionLikeOps[LocalDateTime](o)

  implicit class EndpointLikeOBigDecimal(bd: Option[BigDecimal]) extends EndpointOptionLikeOps[BigDecimal](bd)

  //date time
  def non_empty_datetime = ValidationRule[LocalDateTime]("be non-empty")  { v: LocalDateTime => v != null}
  //Numerics
  def non_empty_numeric[V: Numeric] = ValidationRule[V]("be non-empty") { v: V => v != null}
  def positive[V : Numeric] = ValidationRule[V]("be positive") { _ > 0 }
  def negative[V : Numeric] = ValidationRule[V]("be negative") { _ < 0 }
  def less_than[V : Numeric](v: V) = ValidationRule[V](s"be less than $v") { _ < v }
  def greater_than[V : Numeric](v: V) = ValidationRule[V](s"be greater than $v") { _ > v }
  def between[V : Numeric](start: V, end: V) = ValidationRule[V](s"be between $start and $end") {
    x =>  x >= start && x <= end
  }

  def money[V : Numeric] = ValidationRule[V](s"should be of type 'money'") { m => true}

  def one_of[V: Numeric](v: String) = ValidationRule[V](s"be one of $v") { m =>
    val lst = v.split(",").map(_.trim).toList
    lst.contains(m.toString)
  }


    //Strings
  def non_empty_string = ValidationRule[String]("be non-empty") { s: String =>  s != null && s.size > 0 }
  def is_empty = ValidationRule[String]("be empty") { _.size == 0 }

  def longer_than(v: Int) = ValidationRule[String](s"be longer than $v") { _.size > v }
  def shorter_than(v: Int) = ValidationRule[String](s"be shorter than $v") { _.size < v }

  def min_length(v : Int) = ValidationRule[String](s"at least $v characters") { _.size >= v }

  def max_length(v: Int) = ValidationRule[String](s"no longer than $v characters") { _.size <= v }

  def contain(v: String) = ValidationRule[String](s"contain $v") { _.contains(v) }

  def not_contain(v: String) = ValidationRule[String](s"not contain $v") { !_.contains(v) }

  def one_of(v: String) = ValidationRule[String](s"be one of $v") { m =>
    val list = v.split(",").map(_.trim).toList
    list.contains(m)
  }

  def email = ValidationRule[String]("be a valid email") { m =>
    val re = """^[-a-z0-9!#$%&'*+/=?^_`{|}~]+(\.[-a-z0-9!#$%&'*+/=?^_`{|}~]+)*@([a-z0-9]([-a-z0-9]{0,61}[a-z0-9])?\.)*(aero|arpa|asia|biz|cat|com|coop|edu|gov|info|int|jobs|mil|mobi|museum|name|net|org|pro|tel|travel|[a-z][a-z])$"""
    m.matches(re)
  }

  def phone(country: String="US") = ValidationRule[String](s"be valid phone number") { m: String =>
    try {
      val phoneUtil = PhoneNumberUtil.getInstance
      val parsed = phoneUtil.parse(m, "US")
      phoneUtil.isValidNumber(parsed)
    } catch {
      case e: Exception => false
    }
  }

  //TODO: make this much more thorough
  def url = ValidationRule[String](s"be valid url number") { m: String =>
    try {
      new URL(m)
      true
    } catch {
      case e: Exception => false
    }
  }

  def ip_address = ValidationRule[String](s"be valid IP Address") { m: String =>
    val reg = """(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})"""
    m.matches(reg)
  }


  def social_security = ValidationRule[String]("be a valid Social Security Number") { m: String =>
    val re = "^(?!000|666)[0-8][0-9]{2}(?:-){0,1}(?!00)[0-9]{2}(?:-){0,1}(?!0000)[0-9]{4}$"
    m.matches(re)
  }

  def postal_code(country: String) = ValidationRule[String](s"be a valid postal code for $country") {m: String =>
    if (PostalCodes.codes.contains(country)) {
      m.matches(PostalCodes.codes(country))
    } else {
      if (m == "") {
        true
      } else {
        false
      }
    }
  }

  def country_code = ValidationRule[String](s"be a valid country code") { m: String =>
    Countries.countries.contains(m)
  }

  //TODO: date related
  def date = ???
  def time = ???
  def date_time = ???

  def json = ValidationRule[String](s"should be valid JSON") {m: String =>
    parse(m).getOrElse(null) != null
  }

  def xml = ValidationRule[String](s"should be valid XML") { m: String =>
    try {
      scala.xml.XML.loadString(m)
      true
    } catch {
      case e: Exception => false
    }

  }

  //a list with a minimum number of elements
  def list_min(min_elems: Int) =
    ValidationRule[String](s"should be a list with at least $min_elems elements") { m =>
      try {
        m.split(",").size >= min_elems
      } catch {
        case e: Exception => false
      }
  }

  def list_max(max_elems: Int) =
    ValidationRule[String](s"should be a list with at most $max_elems elements") { m =>
      try {
        m.split(",").size <= max_elems
      } catch {
        case e: Exception => false
      }
    }

  def map = ???

  //regex matching
  def matches(v: String) = ValidationRule[String](s"must match $v") {m: String =>
    m.matches(v)
  }
}
