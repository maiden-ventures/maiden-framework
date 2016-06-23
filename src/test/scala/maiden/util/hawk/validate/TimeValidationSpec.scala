package maiden.util.hawk.validate

import maiden.spec.SpecHelper
import maiden.util.hawk.HeaderValidationMethod
import maiden.util.hawk.TaggedTypesFunctions._
import maiden.util.hawk.params._
import maiden.util.hawk.validate.TimeValidation.{acceptableTimeDelta, validate}
import maiden.util.time.TaggedTypesFunctions.Seconds
import maiden.util.time.Time.{nowUtc, time}
import maiden.util.time._
import org.scalacheck.Prop.forAll
import org.scalacheck.Properties
import org.specs2.mutable.Specification

final class TimeValidationSpec extends Specification with SpecHelper {
  val credentials = Credentials(KeyId("fred"), Key("d0p1h1n5"), Sha256)

  val timestamps = new Properties("Timestamps") {
    property("are valid if within the interval") = forAll { (time: Time) =>
      val delta = nowUtc.minus(time).getStandardSeconds
      if (delta > acceptableTimeDelta.getStandardSeconds) {
        validate(credentials, context(time), HeaderValidationMethod) must beXorLeft
      } else {
        validate(credentials, context(time), HeaderValidationMethod) must beXorRight
      }
    }
  }

  s2"Validating timestamps$timestamps"

  private def context(time: Time): RequestContext = {
    val header = RequestAuthorisationHeader(KeyId("fred"), time, Nonce("nonce"), None, Some(ExtendedData("data")), MAC(Base64Encoded("base64")))
    RequestContext(Get, Host("example.com"), Port(80), UriPath("/"), header, None)
  }
}
