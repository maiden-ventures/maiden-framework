package maiden.models

import java.io.{File, FilenameFilter}
import org.apache.commons.io.filefilter.WildcardFileFilter
import org.joda.time.format.ISODateTimeFormat
import com.imageworks.migration._
import com.zaxxer.hikari.HikariDataSource
import maiden.common.Text.pascalize
import maiden.config.MaidenConfig

object Migrate {

  val namespace  = MaidenConfig.get[String]("migrations.namespace")
  val driver_class_name = MaidenConfig.get[String]("migrations.database_driver")
  val vendor = Vendor.forDriver(driver_class_name)
  val migration_adapter = DatabaseAdapter.forVendor(vendor, None)
  lazy val connectionString = s"jdbc:${MaidenConfig.get[String]("migrations.database_type")}://${MaidenConfig.get[String]("db.dataSource.serverName")}/${MaidenConfig.get[String]("db.dataSource.databaseName")}"
  val dataSource = new HikariDataSource();
  dataSource.setJdbcUrl(connectionString);
  dataSource.setUsername(MaidenConfig.get[String]("db.dataSource.user"));
  dataSource.setPassword(MaidenConfig.get[String]("db.dataSource.password"));

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


  lazy val migrationPath = s"${System.getProperty("user.dir")}/${MaidenConfig.get[String]("app.source_path")}/migrations"

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
  s"""package ${MaidenConfig.get[String]("migrations.namespace")}

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
