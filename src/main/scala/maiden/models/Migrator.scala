package maiden.models

import java.io.{File, FilenameFilter}
import scala.util.Properties
import org.apache.commons.io.filefilter.WildcardFileFilter
import org.joda.time.format.ISODateTimeFormat
import com.imageworks.migration._
import com.typesafe.config.ConfigFactory
import com.zaxxer.hikari.HikariDataSource
import maiden.common.Text.pascalize

object Migrate {

  lazy val  conf = ConfigFactory.load()
  val namespace  = conf.getString("migrations.namespace")
  val driver_class_name = conf.getString("migrations.database_driver")
  val vendor = Vendor.forDriver(driver_class_name)
  val migration_adapter = DatabaseAdapter.forVendor(vendor, None)
  lazy val connectionString = s"jdbc:${conf.getString("migrations.database_type")}://${conf.getString("db.dataSource.serverName")}/${conf.getString("db.dataSource.databaseName")}"
  val dataSource = new HikariDataSource();
  dataSource.setJdbcUrl(connectionString);
  dataSource.setUsername(conf.getString("db.dataSource.user"));
  dataSource.setPassword(conf.getString("db.dataSource.password"));

  lazy val migrator = {
    new Migrator(dataSource, migration_adapter)
  }

  def main(args: Array[String]):Unit =  args match {
      case Array("up") => up
      case Array("clean") => clean
      case Array("down") => down
      case Array("rollback", num) => rollback(num.toInt)
      case Array("generate", name) => generate(name)
      case Array("rebuild") => rebuild
      case _ => ()
  }

  def up() = {
    migrator.migrate(InstallAllMigrations, namespace, false)
  }
  def clean() = {
    down()
  }

  def down() = {
    migrator.migrate(RemoveAllMigrations, namespace, false)
  }

  def rollback(version: Int) = {
    migrator.migrate(MigrateToVersion(version), namespace, false)
  }

  def rebuild() = {
    down()
    up()
    //Seeder.seed()
  }


  lazy val migrationPath = s"${System.getProperty("user.dir")}/${conf.getString("app.source_path")}/migrations"

  private def checkMigrationExists(name:String) = {
    val glob = s"*_${name}.scala"
    val filter: FilenameFilter = new WildcardFileFilter(glob)
    val files = (new File(migrationPath)).listFiles(filter)

    if (files.length > 0) {
      throw(new Exception("There is already a migration for " + name))
    }
  }

  def generate(name: String) {
    val fmt = ISODateTimeFormat.dateHourMinuteSecond()
    val date = fmt.print(System.currentTimeMillis)
                .replace(":", "")
                .replace("-", "")
                .replace("T", "")

    val className = s"Migrate_${date}_${pascalize(name)}"
    val file = s"${migrationPath}/${className.replace("Migrate_", "")}.scala"
    checkMigrationExists(pascalize(name))
    scala.tools.nsc.io.File(file).writeAll(createTemplate(className))
    println(s"New migration in ${file}")
  }

  private def createTemplate(className: String) = {
  s"""package ${conf.getString("migrations.namespace")}

import com.imageworks.migration._

class ${className} extends Migration {

  val table = ""; //put your table name here

  def up() {

  }

  def down() {
    //dropTable(table)
  }
}
"""
  }

}
