package maiden.types

/**
  * Created by glen on 6/20/16.
  */
object Exceptions {



  sealed class MaidenException(_message: String) {
    val  message = _message
  }

  case object MaidenRenderException extends MaidenException("Render Error Occured")
  case object MaidenModelNotFoundException extends MaidenException("Model Not Found")
  case object MaidenModelUnauthorizedException extends MaidenException("Unauthorized")

}
