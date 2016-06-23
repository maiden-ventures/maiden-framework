package maiden.util.config

import maiden.util.error.ErrorReporter

/**
  * An "environment" is a set of environment variables & a name.
  */
trait Environment {
  val name: String
  val isDevelopment: Boolean
  val isTest: Boolean
  val isProduction: Boolean
}
