package maiden

import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response}
import maiden.api.v1.ErrorHandler.apiErrorHandler
import maiden.api.v1.ResponseEncoders
import maiden.api.v1.hello.HelloApi._
import maiden.config.Config.apiAuthenticationCredentials
import maiden.http.{ExceptionFilter, HawkAuthenticateRequestFilter, RequestLoggingFilter}
import maiden.util.error.ErrorResponseEncoders

object AuthenticationFilter extends HawkAuthenticateRequestFilter(apiAuthenticationCredentials)

object UnhandledExceptionsFilter extends ExceptionFilter[Request](ErrorResponseEncoders.exceptionResponseEncoder)

object FinchTemplateApi extends ResponseEncoders {
  private def api = helloApi()

  def apiService: Service[Request, Response] =
    RequestLoggingFilter andThen
      UnhandledExceptionsFilter andThen
      //AuthenticationFilter andThen
      api.handle(apiErrorHandler).toService
}
