package maiden.processing

import java.time.LocalDateTime
import java.net.URL
import scala.util.matching._
import com.twitter.util.{Future, Return, Throw, Try}
import io.finch.internal._
import spire.math._
import spire.implicits._
import io.finch._
import com.google.i18n.phonenumbers._

object Validations {


  //handle implicit conversion from Type to Option[Type]

/* allow for *some* raw values to behave like Finch endpoints
   with regard to validations
*/

  abstract class EndpointLikeOps[T](value: T) {
    def should(name: String)(rule: ValidationRule[T]) =
      if (rule.apply(value)) Future.value(value)
      else Future.exception(Error(s" ${name} should ${rule.description}"))

    def should(rule: ValidationRule[T]): Future[T] = should(s""""${value.toString}"""")(rule)
  }

  abstract class EndpointOptionLikeOps[T](value: Option[T]) {
    def should(name: String)(rule: ValidationRule[T]) =
      value match {
        case Some(value) => {
          if (rule.apply(value)) Future.value(value)
          else Future.exception(Error(s" ${name} should ${rule.description}"))
        }
        case _ =>
          Future.exception(Error(s" ${name} should not be empty"))
      }

    def should(rule: ValidationRule[T]): Future[T] = should(s""""${value.toString}"""")(rule)
  }

  implicit class EndpointLikeString(s: String) extends EndpointLikeOps[String](s)
  implicit class EndpointLikeInt(i: Int) extends EndpointLikeOps[Int](i)
  implicit class EndpointLikeLong(l: Long) extends EndpointLikeOps[Long](l)
  implicit class EndpointLikeDouble(d: Double) extends EndpointLikeOps[Double](d)
  implicit class EndpointLikeFloat(f: Float) extends EndpointLikeOps[Float](f)
  implicit class EndpointLikeLocalDateTime(d: LocalDateTime) extends EndpointLikeOps[LocalDateTime](d)


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


  //Numerics
  def positive[V : Numeric] = ValidationRule[V]("be positive") { _ > 0 }
  def negative[V : Numeric] = ValidationRule[V]("be negative") { _ < 0 }
  def less_than[V : Numeric](v: V) = ValidationRule[V](s"be less than $v") { _ < v }
  def greater_than[V : Numeric](v: V) = ValidationRule[V](s"be greater than $v") { _ > v }
  def between[V : Numeric](start: V, end: V) = ValidationRule[V](s"be between $start and $end") {
    x =>  x >= start && x <= end
  }

  //Strings
  def non_empty = ValidationRule[String]("be non-empty") { _.size > 0 }
  def is_empty = ValidationRule[String]("be empty") { _.size == 0 }
  def longer_than(v: Int) = ValidationRule[String](s"be longer than $v") { _.size > v }
  def shorter_than(v: Int) = ValidationRule[String](s"be shorter than $v") { _.size < v }

  def min_length(v : Int) = ValidationRule[String](s"at least $v characters") { _.size >= v }

  def max_length(v: Int) = ValidationRule[String](s"no longer than $v characters") { _.size <= v }

  def contain(v: String) = ValidationRule[String](s"contain $v") { _.contains(v) }

  def not_contain(v: String) = ValidationRule[String](s"not contain $v") { !_.contains(v) }

  def email = ValidationRule[String]("be a valid email") { m =>
    val re = """^[-a-z0-9!#$%&'*+/=?^_`{|}~]+(\.[-a-z0-9!#$%&'*+/=?^_`{|}~]+)*@([a-z0-9]([-a-z0-9]{0,61}[a-z0-9])?\.)*(aero|arpa|asia|biz|cat|com|coop|edu|gov|info|int|jobs|mil|mobi|museum|name|net|org|pro|tel|travel|[a-z][a-z])$""".r

    if (re.findFirstIn(m) == None)
      false
    else
      true
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

  def social_security = ValidationRule[String]("be a valid Social Security Number") { m: String =>
    val re = "^(?!000|666)[0-8][0-9]{2}(?:-){0,1}(?!00)[0-9]{2}(?:-){0,1}(?!0000)[0-9]{4}$".r
    re.findFirstIn(m) match {
      case Some(x) => true
      case _ => false
    }
  }

  def postal_code(country: String = "US") = ValidationRule[String](s"be a valid Url") {m: String =>
       false
  }

  //TODO: date related
  def date = ???
  def time = ???
  def date_time = ???

  //TODO: is the string valid json or xml
  def json = ???
  def xml = ???

  //TODO: is this a Sequence of items
  def list = ???
  def map = ???

  //match via some function (String) => Boolean
  def matches(s: => (String) => Boolean) = ValidationRule[String](s"must match $s") { m: String =>
    s(m)
  }
  //regex matching
  def must_match(v: String) = ValidationRule[String](s"must match $v") {m: String =>
    m match {
      case _ => false
    }
  }
}
