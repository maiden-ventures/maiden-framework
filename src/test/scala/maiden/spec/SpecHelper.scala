package maiden.spec

import maiden.config.Config
import maiden.spec.gen.Generators
import maiden.util.json.JsonCodecOps
import maiden.util.log.Logger
import org.specs2.ScalaCheck
import org.specs2.execute.Failure
import org.specs2.matcher.XorMatchers
import org.specs2.specification.BeforeAll

trait SpecHelper extends TestEnvironmentSetter with ScalaCheck with Generators with JsonCodecOps with XorMatchers with BeforeAll {
  val log = new Logger(s"${Config.coreLoggerName}-test")

  override def beforeAll() = setEnvironment()

  def fail(message: String, t: Throwable) = Failure(message, "", t.getStackTrace.toList)
}
