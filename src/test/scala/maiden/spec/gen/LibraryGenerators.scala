package maiden.spec.gen

import maiden.spec.gen.DomainObjectGenerators._
import maiden.util.time.Time.utcTime
import org.joda.time.DateTime
import org.scalacheck.Arbitrary

trait LibraryGenerators {
  val genDateTime = genMillis.map(m => utcTime(m).asDateTime)

  implicit def arbDateTime: Arbitrary[DateTime] = Arbitrary(genDateTime)
}
