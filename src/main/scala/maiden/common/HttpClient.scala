/* an HTTP client helper based on http4s */
package maiden.common

import java.util.concurrent.ScheduledExecutorService
import java.io._
import java.util.concurrent.TimeoutException
import scala.concurrent.duration._
import org.http4s._
import org.http4s.util.CaseInsensitiveString
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


trait MaidenHttpResponse

case class StringHttpResponse(
  headers: Map[String, String],
  body: String
) extends MaidenHttpResponse

case class JsonHttpResponse(
  headers: Map[String, String],
  body: JValue
) extends MaidenHttpResponse

case class MapHttpResponse(
  headers: Map[String, String],
  body: Map[String, Any]
) extends MaidenHttpResponse

case class ByteVectorHttpResponse(
  headers: Map[String, String],
  body: ByteVector
) extends MaidenHttpResponse

case class FileHttpResponse(
  headers: Map[String, String],
  body: File
) extends MaidenHttpResponse

class HttpClient(url: String,
                 timeout: Duration = 30 second,
                 method: String = "GET",
                 data: Map[String, Seq[String]] = Map.empty,
                 headers: Map[String, String] = Map.empty ) {

  implicit val formats = DefaultFormats
  def bv2str(bv: ByteVector) = bv.toIterable.map(_.toChar).mkString("")
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

  def fetchHeadersOnly(headerList: List[String] = List.empty) = {
    val res = client.map {
      case NotFound(resp) => throw(new UrlNotFoundException(message=url))
      case resp => resp
    }

    val resp = res.runFor(30000)//unsafePerformSync

    if (headerList == List.empty) {
      resp.headers.map(h =>  h.name.toString -> h.value).toMap
    } else {
      headerList.map(hl => {
        val  header = resp.headers.get(CaseInsensitiveString(hl))
        header.map(h =>
          h.name.toString -> h.value.toString
        ).get
      }).toMap
    }
  }

  def buildHeaderMap(resp: Response) =
    resp.headers.map(h =>  h.name.toString -> h.value).toMap

  def fetchRaw() = {
    val res = client.map{
      //case Successful(resp) => resp.as[ByteVector]
      case NotFound(resp) => throw(new UrlNotFoundException(message=url))
      case resp => resp//.as[ByteVector]
    }

    try {
      val resp = res.runFor(30000)//unsafePerformSync //Timed(10)
      val headers = buildHeaderMap(resp)
      val p = resp.body
      val p2 = p.runLog

      val rawBody = resp.body.runLog.run //unsafePerformSync //run

      //handle chunked responses
      var body:ByteVector = ByteVector.empty
      rawBody.foreach { x => body = body ++ x}

      //r => r.runLast.runLast } //.as[ByteVector] }
      ByteVectorHttpResponse(headers, body)

    } catch {
      case e: TimeoutException  => throw(new ExternalResponseTimeoutException(message = url))
      case e: Exception =>  {
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
  private[this] def fetchAsString() = {
    val response =  fetchRaw
    StringHttpResponse(response.headers, bv2str(response.body))
  }

  def fetch() = fetchAsString
  //def fetch[T](callback : (ByteVector) => T) = callback(fetchRaw)
  def fetchAsMap() = {
    val response = fetchRaw
    MapHttpResponse(response.headers, asMap(response.body))
  }
  def fetchAsJson() = {
    val response = fetchRaw
    JsonHttpResponse(response.headers, asJson(response.body))
  }

  /* fetch a resource from a URL and save as a file
  works with binary and text resources */
  def fetchAsFile(fileName: String) = {
    val response = fetchRaw
    val r = response.body.toIterable.toArray
		FileWriter.write(r, fileName)
		FileHttpResponse(response.headers, new File(fileName))
  }
}
