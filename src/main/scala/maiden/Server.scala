package maiden

import java.io.{File, FileNotFoundException, FileOutputStream}
import java.nio.file.{Paths, Files}
import java.lang.management.ManagementFactory
import java.net.{InetSocketAddress, SocketAddress}
import com.twitter.app.App
import com.twitter.conversions.storage.intToStorageUnitableWholeNumber
import com.twitter.finagle._
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.netty3.Netty3ListenerTLSConfig
import com.twitter.finagle.param.Label
import com.twitter.finagle.ssl.Ssl
import com.twitter.server.Lifecycle.Warmup
import com.twitter.server.{Admin, AdminHttpServer, Lifecycle, Stats}
import com.twitter.util.{Await, CountDownLatch}
import maiden.util.log.Logger
import maiden.config.MaidenConfig


/* ****VERY**** HEAVILY BORRROWED FROM https://github.com/BenWhitehead/finch-server/blob/master/src/main/scala/io/github/benwhitehead/finch/FinchServer.scala */

trait MaidenServer extends App
  with AdminHttpServer
  with Admin
  with Lifecycle
  with Warmup
  with Stats {

  //grab all of our config variables

  lazy val rootPath = System.getProperty("user.dir")
  lazy val serverName = MaidenConfig.get[String]("app.id")

  //lazy val logger = org.slf4j.LoggerFactory.getLogger(getClass.getName)
  lazy val logger = new Logger(serverName)

  lazy val pid: String = ManagementFactory.getRuntimeMXBean.getName.split('@').head

  implicit val stats = statsReceiver

  case class MaidenServerConfig (
    httpInterface: Option[InetSocketAddress],
    pidPath: String,
    httpsInterface: Option[InetSocketAddress],
    certificatePath: String,
    keyPath: String,
    maxRequestSize: Int
  )

  private[this] def buildInterface(key: String): Option[InetSocketAddress] = {
    println(MaidenConfig.getOption[String](key))
    MaidenConfig.getOption[String](key) match {
      case Some(portSpec) if portSpec !="" =>
            Option(new InetSocketAddress("0.0.0.0", portSpec.toInt))
      case _ => None
    }
  }

  private[this] def buildConfig() = {
    val httpInterface = buildInterface("app.server.http_interface")
    val httpsInterface = buildInterface("app.server.https_interface")
    val certificatePath = MaidenConfig.getOption[String]("app.server.certificate_path", "")

    val keyPath = MaidenConfig.getOption[String]("app.server.key_path", "")
    val pidPath = MaidenConfig.getOption[String]("app.server.pid_path", s"${rootPath}/pids/${serverName}.pid")

    val maxRequestSize = MaidenConfig.getOption[Int]("app.server.max_request_size", 5)

    MaidenServerConfig(
      httpInterface = httpInterface,
      httpsInterface = httpsInterface,
      certificatePath = certificatePath,
      keyPath = keyPath,
      pidPath = pidPath,
      maxRequestSize = maxRequestSize
    )
  }

  def service: Service[Request, Response]

  lazy val config = buildConfig

  @volatile private var server: Option[ListeningServer] = None
  @volatile private var tlsServer: Option[ListeningServer] = None
  private val cdl = new CountDownLatch(1)

  def writePidFile() = {
    if (!Files.exists(Paths.get(config.pidPath))) {
      val f = new File(config.pidPath)
      f.mkdirs()

    }
    val pidFile = new File(config.pidPath)
    val pidFileStream = new FileOutputStream(pidFile)
    pidFileStream.write(pid.getBytes)
    pidFileStream.close()
  }

  def removePidFile() = {
    val pidFile = new File(config.pidPath)
    pidFile.delete()
  }

  def main(): Unit = {
    if (!config.pidPath.isEmpty) {
      writePidFile()
    }
    logger.info("process " + pid + " started")

    logger.info(s"admin http server started on: ${adminHttpServer.boundAddress}")
    server = startServer()
    server foreach { ls =>
      logger.info(s"http server started on: ${ls.boundAddress}")
      closeOnExit(ls)
    }

    if (!config.certificatePath.isEmpty && !config.keyPath.isEmpty) {
      verifyFileReadable(config.certificatePath, "SSL Certificate")
      verifyFileReadable(config.keyPath, "SSL Key")
      tlsServer = startTlsServer()
    }

    tlsServer foreach { ls =>
      logger.info(s"https server started on: ${ls.boundAddress}")
      closeOnExit(_)
    }
    cdl.countDown()

    (server, tlsServer) match {
      case (Some(s), Some(ts)) => Await.all(s, ts)
      case (Some(s), None)     => Await.all(s)
      case (None, Some(ts))    => Await.all(ts)
      case (None, None)        => throw new IllegalStateException("No server to wait for startup")
    }
  }

  def awaitServerStartup(): Unit = {
    cdl.await()
  }

  def serverPort: Int = {
    assert(cdl.isZero, "Server not yet started")
    (server map { case s => getPort(s.boundAddress) }).get
  }
  def tlsServerPort: Int = {
    assert(cdl.isZero, "TLS Server not yet started")
    (tlsServer map { case s => getPort(s.boundAddress) }).get
  }

  def startServer(): Option[ListeningServer] = {
    config.httpInterface.map { iface =>
      val name = s"http/$serverName"
      Http.server
        .configured(Label(name))
        .configured(Http.param.MaxRequestSize(config.maxRequestSize.megabytes))
        .serve(iface, getService(s"srv/$name"))
    }
  }

  def startTlsServer(): Option[ListeningServer] = {
    config.httpsInterface.map { iface =>
      val name = s"https/$serverName"
      Http.server
        .configured(Label(name))
        .configured(Http.param.MaxRequestSize(config.maxRequestSize.megabytes))
        .withTls(Netty3ListenerTLSConfig(() => Ssl.server(config.certificatePath, config.keyPath, null, null, null)))
        .serve(iface, getService(s"srv/$name"))
    }
  }

  def getService(serviceName: String): Service[Request, Response] = {
    service
    //(new StatsFilter(serviceName)) andThen service
  }

  onExit {
    removePidFile()
  }

  private[this] def getPort(s: SocketAddress): Int = {
    s match {
      case inet: InetSocketAddress => inet.getPort
      case _ => throw new RuntimeException(s"Unsupported SocketAddress type: ${s.getClass.getCanonicalName}")
    }
  }
  private def verifyFileReadable(path: String, description: String): Unit = {
    val file = new File(path)
    if (file.isFile && !file.canRead){
      throw new FileNotFoundException(s"$description could not be read: $path")
    }
  }
}
