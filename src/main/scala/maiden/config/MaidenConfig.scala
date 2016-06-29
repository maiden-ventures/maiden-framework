package maiden.config

import java.io.File
import scala.util.Properties
import scala.collection.JavaConversions._
import com.typesafe.config.{ConfigFactory, ConfigValueType}

object MaidenConfig {

  lazy val env = Properties.envOrElse("MAIDEN_MODE", "development")
  lazy val _conf = ConfigFactory.parseFile(new File(s"config/maiden-${env}.conf"))
  lazy val conf = ConfigFactory.load(_conf)

  def get[T](key: String): T = {
    conf.getValue(key).unwrapped.asInstanceOf[T]
  }

  def getOption[T](key: String): Option[T] = try {
    Option(get[T](key))
  } catch {
    case e: Throwable => None
  }

  //get all key-value pairs where key starts with (or is) group
  def getGroup(group: String): Map[String, Any] = {
    conf
      .entrySet
      .filter(e => e.getKey.startsWith(group))
      .toList
      .map(e => e.getKey -> e.getValue.unwrapped).toMap
  }

  def apply(group: String) = {
    getGroup(group)
  }
}
