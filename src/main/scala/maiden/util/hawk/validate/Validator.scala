package maiden.util.hawk.validate

import cats.data.Xor
import maiden.util.hawk.{HawkError, ValidationMethod}
import maiden.util.hawk.params.RequestContext

trait Validator[T] {
  def validate(credentials: Credentials, context: RequestContext, method: ValidationMethod): Xor[HawkError, T]
}
