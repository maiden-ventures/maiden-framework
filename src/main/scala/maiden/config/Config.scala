package maiden.config

import com.twitter.finagle.stats.{LoadedStatsReceiver, StatsReceiver}
import maiden.util.config.ConfigUtils
import maiden.util.hawk.TaggedTypesFunctions._
import maiden.util.hawk.validate.{Credentials, Sha256}

trait MonitoringConfig extends ConfigUtils {
  val rollbarAccessKey = envVarOrFail("ROLLBAR_ACCESS_KEY")
}

trait SystemConfig extends ConfigUtils {
  lazy val statsReceiver: StatsReceiver = LoadedStatsReceiver

  val systemId = "maiden-framework"

  val coreLoggerName = systemId

  def environment = envVarOrFail("ENV")

  def listenAddress = s":${envVarOrFail("PORT")}"

  val apiAuthenticationCredentials = Credentials(KeyId("API Client"), Key("4ef04c842e178c502e8ae4fa7d14dc6f"), Sha256)

  val miscThreadPoolSize = 100
}

object Config extends SystemConfig with MonitoringConfig with ConfigUtils
