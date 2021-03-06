package maiden.util.hawk

import cats.data.Xor
import maiden.util.hawk.params.{PayloadContext, RequestContext}
import maiden.util.hawk.parse.RequestAuthorisationHeaderParser
import maiden.util.hawk.validate.MacValidation.{validate => validateMac}
import maiden.util.hawk.validate.TimeValidation.{validate => validateTime}
import maiden.util.hawk.validate._

sealed trait ValidationMethod {
  def identifier: String
}

case object HeaderValidationMethod extends ValidationMethod {
  override val identifier = "hawk.1.header"
}

case object PayloadValidationMethod extends ValidationMethod {
  override val identifier = "hawk.1.payload"
}

trait RequestValid

object HawkAuthenticate {
  /**
    * Parse a Hawk `Authorization` request header.
    **/
  def parseRawRequestAuthHeader(header: RawAuthenticationHeader): Option[RequestAuthorisationHeader] =
    RequestAuthorisationHeaderParser.parseAuthHeader(header)

  /**
    * Authenticate an incoming request using Hawk.
    **/
  def authenticateRequest(credentials: Credentials, context: RequestContext, method: ValidationMethod = PayloadValidationMethod): Xor[HawkError, RequestValid] =
    validateTime(credentials, context, method).map(_ => validateMac(credentials, context, method)).map(_ => new RequestValid {})

  /**
    * Authenticate an outging response into a form suitable for adding to a `Server-Authorization` header.
    */
  def authenticateResponse(credentials: Credentials, payload: Option[PayloadContext]): ServerAuthorisationHeader = ???
}
