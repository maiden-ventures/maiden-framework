package maiden.api.v1.hello

import com.twitter.finagle.http.Message
import com.twitter.finagle.http.Message.ContentTypeJson
import com.twitter.io.Buf._
import io.circe.Encoder
import io.circe.syntax._
import io.circe.generic.auto._
import io.finch.EncodeResponse

trait HelloResponseEncoders {

  /*Uimplicit val helloEncoder = Encoder.instance[Hello] { h =>
    Map("id" -> h.id,  "name" -> h.name).asJson
  }
   */

  implicit def helloResponseEncoder: EncodeResponse[Hello] =
    EncodeResponse(ContentTypeJson)(hello => Utf8(Map("payload" -> hello).asJson.noSpaces))

}

object HelloResponseEncoders extends HelloResponseEncoders
