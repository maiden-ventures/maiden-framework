package maiden.api.v1

import maiden.api.v1.hello.HelloResponseEncoders
import maiden.util.error.ErrorResponseEncoders

trait ResponseEncoders extends ErrorResponseEncoders with HelloResponseEncoders

object ResponseEncoders extends ResponseEncoders
