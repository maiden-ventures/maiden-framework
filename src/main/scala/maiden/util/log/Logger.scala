package maiden.util.log

import maiden.config.MaidenConfig
import org.slf4j.LoggerFactory

trait Logging {
  lazy val log = new Logger(MaidenConfig.get[String]("app.name"))
}

object Logger extends Logging

final class Logger(name: String) {

  lazy val log = LoggerFactory.getLogger(name)

  def info(s: String) {
    log.info(s)
  }

  def warn(s: String) {
    log.warn(s)
  }

  def warnST(s: String, t: Throwable) {
    log.warn(s, t)
  }

  def error(s: String) {
    log.error(s)
  }

  def errorST(s: String, t: Throwable) {
    log.error(s, t)
  }
}
