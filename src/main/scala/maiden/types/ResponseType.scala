package maiden.types

import maiden.util.http.HttpOps
import maiden.util.log.Logging


/* must be extended by all responses */
sealed trait ResponseType

/* same  but for errors */
sealed trait ErrorResponseType extends ResponseType


trait MaidenApi extends HttpOps with Logging
