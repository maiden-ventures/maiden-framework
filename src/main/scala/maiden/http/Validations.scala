package maiden.http

import scala.util.matching._
import com.twitter.util.{Future, Return, Throw, Try}
import io.finch.internal._
import spire.math._
import spire.implicits._
import io.finch._


//pimp String-like and Integral to support "should"


object Validations {

abstract class EndpointLikeOps[T](value: T) {
    def should(rule: ValidationRule[T]) =
      if (rule.apply(value)) Future.value(value)
      else Future.exception(new Exception(s" ${value} should ${rule}"))
}

  implicit class EndpointLikeString(s: String) extends EndpointLikeOps[String](s)
  implicit class EndpointLikeInt(i: Int) extends EndpointLikeOps[Int](i)
  implicit class EndpointLikeLong(l: Long) extends EndpointLikeOps[Long](l)



  val predefinedRegexes = Map(
    "email" -> None,
    "phone" -> None,
    "postal_code" -> None,
    "country_code" -> None,
    "url" ->  None
  )

  //Integers/Longs
  def positive[V : Integral] = ValidationRule[V]("be positive") { x =>
    x > 0
  }

  def negative[V : Integral] = ValidationRule[V]("be negative) { x =>
    x < 0
  }


  def less_than[V : Integral](v: V) = ValidationRule[V](s"be less than $v") { x =>
    x < v
  }

  def greater_than[V : Integral](v: V) = ValidationRule[V](s"be greater than $v") { x=>
    x > v
  }

  def between[V : Integral](start: V, end: V) = ValidationRule[V](s"be between $start and $end") {
    x =>  x >= start && x <= end
  }


  //Strings
  def non_empty = ValidationRule[String]("be non-empty") { _.size > 0 }
  def is_empty = ValidationRule[String]("be empty") { _.size == 0 }
  def longer_than(v: Int) = ValidationRule[String](s"be longer than $v") { _.size > v }
  def shorter_than(v: Int) = ValidationRule[String](s"be shorter than $v") { _.size < v }

  def min_length(v : Int) = ValidationRule[String](s"at least $v characters") { _.size >= v }

  def max_length(v: Int) = ValidationRule[String](s"no longer than $v characters") { _.size <= v }

  //regex matching
  def must_match(v: String) = ValidationRule[String](s"must match $v") {m: String =>
    m match {
      case s if predefinedRegexes.contains(m) => true//predefined regex
      case _ => true //customm regex
    }
  }

}
