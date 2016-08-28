/* an HTTP client helper based on http4s */
package maiden.common

import java.util.concurrent.ScheduledExecutorService
import java.io._
import java.util.concurrent.TimeoutException
import scala.concurrent.duration._
import org.http4s._
import org.http4s.Method._
import org.http4s.client._
import org.http4s.util.{UrlCodingUtils, UrlFormCodec}
import org.http4s.client.blaze._
import scodec.bits.ByteVector
import scalaz.concurrent.Task
import scalaz.stream.Process
import org.http4s.Status.NotFound
import org.http4s.Status.ResponseClass.Successful
import org.json4s._
import org.json4s.native.JsonMethods._
import maiden.exceptions._

class HttpClient(url: String,
                 timeout: Duration = 30 second,
                 method: String = "GET",
                 data: Map[String, Seq[String]] = Map.empty,
                 headers: Map[String, String] = Map.empty ) {

  implicit val formats = DefaultFormats
  def bv2str(bv: ByteVector) = bv.toIterable.map(_.toChar).mkString("")
  //implicit def bv2str(bv: ByteVector): String = bv.toIterable.map(_.toChar).mkString("")
  def bv2jsoninput(bv: ByteVector): JsonInput = bv2str(bv)


  val baseClient = middleware.FollowRedirect(1)(defaultClient)

  val params = data.map { case (k, v) => s"${k}=${v(0)}" }.mkString("&")

  def buildRequest() = {

    val bv = if (data.size == 1 && data.contains("json")) {
      //special case where we are posting pure json
      ByteVector.encodeUtf8(data("json")(0))
    } else {
      ByteVector.encodeUtf8(params)
    }

    val p = Process.emit(
      bv match {
        case Right(x) => x
        case _ => throw(new Exception("Invalid Post Data"))
      }
    )

    val realHeaders = headers.map { case(k,v) => Header(k,v) }.toList
    val h = Headers(realHeaders)
    method match {
      case "POST" => Request(method = POST, uri = getUri(url), headers = h, body = p)
      case "GET" => Request(method = GET, uri = getUriWithParams(url), headers=h)
      case "PUT" => Request(method = PUT, uri = getUri(url), headers = h, body = p)
      case "DELETE" => Request(method = DELETE, uri =  getUri(url) , headers = h, body = p)
      case _ => Request(method = GET, uri = getUri(url))
    }
  }

  val client = baseClient(buildRequest)

  def getUri(s: String): Uri =
    Uri.fromString(s).getOrElse(throw(new InvalidUrlException(message=s)))


  def getUriWithParams(s: String) = {
    val paramStr = if (params.length > 0) {
      s"?${params}"
    } else {
      ""
    }
    getUri(s"${s}${paramStr}")
  }

  def fetchRaw() = {
    val res = client.flatMap {
      case Successful(resp) => resp.as[ByteVector]
      case NotFound(resp) => throw(new UrlNotFoundException(message=url))
      case resp => throw(new ExternalResponseException(message = resp.toString))
    }

    try {
      res.timed(timeout).run
    } catch {
      case e: TimeoutException  => throw(new ExternalResponseTimeoutException(message = url))
      case e: Exception =>  {
        println(e)
        throw(new ExternalResponseException(message = e.getMessage, exc=Option(e)))
      }
    }
  }

  private[this] def asJson(s: ByteVector) = {
    try {
      parse(bv2str(s))
    } catch {
      case e: Exception => throw(new ExternalResponseException(
                              message = url, exc = Option(e)))
    }
  }

  private[this] def asMap(s: ByteVector) = try {
    asJson(s).extract[Map[String, Any]]
  } catch {
    case e: Exception => throw(new ExternalResponseException(
                            message = url, exc = Option(e)))
  }
  //use implicit to convert ByteVector => String
  private[this] def fetchAsString():String = bv2str(fetchRaw)

  def fetch() = fetchAsString
  def fetch[T](callback : (ByteVector) => T) = callback(fetchRaw)
  def fetchAsMap() = fetch(asMap)
  def fetchAsJson() = fetch(asJson)

  /* fetch a resource from a URL and save as a file
  works with binary and text resources */
  def fetchAsFile(fileName: String) = {
    val r = fetchRaw.toIterable.toArray
		FileWriter.write(r, fileName)
		new File(fileName)
  }
}
