package maiden.util.hawk.validate

import maiden.util.hawk._

final case class Credentials(keyId: KeyId, key: Key, algorithm: Algorithm)
