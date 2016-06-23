package maiden.util.hawk.validate

import maiden.util.hawk._

final case class ServerAuthorisationHeader(mac: MAC, payloadHash: PayloadHash, extendedData: ExtendedData)
