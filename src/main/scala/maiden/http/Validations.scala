package maiden.http

import scala.util.matching._
import spire.math._
import spire.implicits._
import io.finch._

object Validations {

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
  def non_empty[S <: String] = ValidationRule[S]("be non-empty") { _.size > 0 }
  def is_empty[S <: String] = ValidationRule[S]("be empty") { _.size == 0 }
  def longer_than[S <: String](v: Int) = ValidationRule[S](s"be longer than $v") { _.size > v }
  def shorter_than[S <: String](v: Int) = ValidationRule[S](s"be shorter than $v") { _.size < v }

  def min_length[S <: String](v : Int) = ValidationRule[S](s"at least $v characters") { _.size >= v }

  def max_length[S <: String](v: Int) = ValidationRule[S](s"no longer than $v characters") { _.size <= v }

  //regex matching
  def must_match[S <: String](v: String) = ValidationRule[S](s"must match $v") {m: String =>
    m match {
      case s if predefinedRegexes.contains(m) => true//predefined regex
      case _ => true //customm regex
    }
  }

}
