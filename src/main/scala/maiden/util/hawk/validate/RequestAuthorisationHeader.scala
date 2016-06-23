package maiden.util.hawk.validate

import maiden.util.hawk._
import maiden.util.time.Time

final case class RequestAuthorisationHeader(keyId: KeyId, timestamp: Time, nonce: Nonce, payloadHash: Option[PayloadHash],
  extendedData: Option[ExtendedData], mac: MAC)
