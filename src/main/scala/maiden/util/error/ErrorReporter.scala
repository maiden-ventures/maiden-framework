
package maiden.util.error

import com.rollbar.Rollbar
import maiden.util.async.AsyncOps.runAsync
import maiden.config.MaidenConfig
import maiden.util.log.Logger.log

trait ErrorReporter {
  def registerForUnhandledExceptions(): Unit

  def info(t: Throwable): Unit

  def warning(t: Throwable): Unit

  def error(t: Throwable): Unit
}

class ConsoleErrorReporter extends ErrorReporter {

  def registerForUnhandledExceptions() = {
    Thread.currentThread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
      def uncaughtException(t:Thread, e: Throwable) {
        log.error(e.getMessage, e)
      }
    });
  }
  def info(t: Throwable) = log.info(t.getMessage)

  def warning(t: Throwable) = log.warn(t.getMessage, t)

  def error(t: Throwable) = log.error(t.getMessage, t)

}

final class RollbarErrorReporter(accessToken: String, environment: String) extends ErrorReporter {
  private lazy val rollbar = new Rollbar(accessToken, environment)

  override def registerForUnhandledExceptions() = rollbar.handleUncaughtErrors()

  override def info(t: Throwable) = runAsync(rollbar.info(t))

  override def warning(t: Throwable) = runAsync(rollbar.warning(t))

  override def error(t: Throwable) = runAsync(rollbar.error(t))
}

object ErrorReporter {
  val environment = MaidenConfig.get[String]("app.environment")
  val rollbar_access_key = MaidenConfig.getOption[String]("app.rollbar_access_key")
  val errorReporter: ErrorReporter = rollbar_access_key match {
    case Some(rollbarAccessKey) => new RollbarErrorReporter(rollbarAccessKey, environment)
    case _ => new ConsoleErrorReporter
  }
}
