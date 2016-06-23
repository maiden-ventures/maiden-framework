package maiden

import com.twitter.finagle.param.Stats
import com.twitter.finagle.{Http, ListeningServer}
import com.twitter.server.util.{JvmStats, TwitterStats}
import com.twitter.util.Await
import maiden.config.Config
import maiden.config.Environment.env
import maiden.util.async.AsyncOps
import maiden.util.error.ErrorReporter._
import maiden.util.log.Logger._


import maiden.models.Reflector

final class App {
  lazy val server: ListeningServer = Http.server
    .withLabel(Config.systemId)
    .configured(Stats(Config.statsReceiver))
    .serve(Config.listenAddress, FinchTemplateApi.apiService)

  def boot(): Unit = {

    log.info(s"Booting ${Config.systemId} in ${env.name} mode on ${server.boundAddress}")
    sys.addShutdownHook(shutdown())
    registerMetricsAndMonitoring()
    Await.ready(server)
  }

  private def registerMetricsAndMonitoring(): Unit = {
    errorReporter.registerForUnhandledExceptions()
    JvmStats.register(Config.statsReceiver)
    TwitterStats.register(Config.statsReceiver)
  }

  private def shutdown(): Unit = {
    log.info(s"${Config.systemId} is shutting down...")
    Await.ready(server.close())
  }
}

object App {
  def main(args: Array[String]): Unit = {
    new App().boot()
  }
}
