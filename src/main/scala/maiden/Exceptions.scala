package maiden.exceptions

import io.netty.handler.codec.http.{HttpResponseStatus => H}

class MaidenException(
    message: String,
    _httpStatus: H = H.BAD_REQUEST,
    exc: Option[Exception] = None)
  extends RuntimeException(message) {

  val httpStatus = _httpStatus
  val underlyingException = exc
}

/* specialized exceptions for the Maiden API */
class UnauthorizedException(
    message: String = "UNAUTHORIZED",
    status: H = H.UNAUTHORIZED,
    exc: Option[Exception]=None)
  extends MaidenException(message, status, exc)

class UserAlreadyExistsException(
    message: String = "The user already exists",
    exc: Option[Exception] = None)
  extends MaidenException(message, exc=None)

class UserAccountMissingIdentityException(
    message: String = "The user does not have an identity account",
    exc: Option[Exception] = None)
  extends MaidenException(message, exc=None)


class CreateOrUpdateFailedException(
    message: String = "The create/update operation failed",
    httpStatus: H = H.INTERNAL_SERVER_ERROR,
    exc: Option[Exception] = None)
  extends MaidenException(message, httpStatus, exc)


class NoUserException(
    message: String = "The user cannot be found",
    httpStatus: H = H.NOT_FOUND,
    exc: Option[Exception] = None)
  extends MaidenException(message, httpStatus, exc)


class InvalidUrlException(
    message: String = "The URL is not in a valid format",
    httpStatus: H = H.BAD_REQUEST,
    exc: Option[Exception] = None)
  extends MaidenException(message, httpStatus, exc)

class UrlNotFoundException(
    message: String = "The given URL cannot be found",
    httpStatus: H = H.BAD_REQUEST,
    exc: Option[Exception] = None)
  extends MaidenException(message, httpStatus, exc)


class FileUploadFailedException(
    message: String = "Unable to upload file",
    httpStatus: H = H.INTERNAL_SERVER_ERROR,
    exc: Option[Exception] = None)
  extends MaidenException(message, httpStatus, exc)

class FacebookFailureException(
    message: String = "Unable to retrieve data from Facebook",
    exc: Option[Exception] = None)
  extends MaidenException(message, exc = None)


class MissingParameterException(
    message: String = "A required parameter is missing",
    exc: Option[Exception] = None)
  extends MaidenException(message, exc = exc)

class ExternalResponseException(
    message: String = "The external resource returned an unparseable result",
    exc: Option[Exception] = None)
  extends MaidenException(message, exc = exc)


class ExternalResponseTimeoutException(
    message: String = "Fetching the external resource timed out",
    exc: Option[Exception] = None)
  extends MaidenException(message, exc = exc)

class ResponseException(
    message: String = "The API returned an invalid response",
    exc: Option[Exception] = None)
  extends MaidenException(message, exc = exc)

class DatasourceNotAvailableException(
    message: String = "FATAL: Unable to connect to data source",
    exc: Option[Exception] = None)
  extends MaidenException(message, exc = exc)
