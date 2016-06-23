package maiden.http

import com.twitter.util.Future
import maiden.auth.AuthToken._
import maiden.auth.Authenticate._
import maiden.auth.AuthenticatedClient
import maiden.util.error.Errors._
import io.finch.Output.payload
import io.finch.{Endpoint, _}

trait OptionalAuthRequestReader {
  /**
    * An endpoint that pulls authentication information out of the request, and won't fail if it isn't present. You
    * might use this in an idempotent sign in endpoint for example.
    *
    * If successful, will pass an `Option[AuthenticatedClient]` to the block:
    *
    * def hello: Endpoint[Hello] =
    *   get("v1" :: "hello" :: string("name") :: optionalAuthorise) { (name: String, c: Option[AuthenticatedClient]) =>
    *     Ok(Hello(name))
    *   }
    */
  val optionalAuthorize: Endpoint[Option[AuthenticatedClient]] =
    paramOption(queryStringAuthToken).mapOutputAsync { maybeToken =>
      Future.value(payload(maybeToken.map(t => AuthenticatedClient(authToken(t)))))
    }
}

object OptionalAuthRequestReader extends OptionalAuthRequestReader

trait RequiredAuthRequestReader {
  /**
    * An endpoint that fails if an authentication token is not provided. If successful, will pass
    * an `AuthenticatedClient` to the block:
    *
    * def hello: Endpoint[Hello] =
    *   get("v1" :: "hello" :: string("name") :: authorise) { (name: String, c: AuthenticatedClient) =>
    *     Ok(Hello(name))
    *   }
    */
  val authorize: Endpoint[AuthenticatedClient] =
    paramOption(queryStringAuthToken).mapOutputAsync { maybeToken =>
      maybeToken.map(t => AuthenticatedClient(authToken(t))) match {
        case Some(c) => authorized(c)
        case None => unauthorized
      }
    }

  private def authorized(c: AuthenticatedClient): Future[Output[AuthenticatedClient]] = Future.value(payload(c))

  private def unauthorized: Future[Output[AuthenticatedClient]] =
    Future.value(Unauthorized(authFailedError(s"Missing auth token; include '$queryStringAuthToken")))
}

object RequiredAuthRequestReader extends RequiredAuthRequestReader
