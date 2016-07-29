package maiden.models

import java.util.Properties
import java.io.PrintWriter
import com.zaxxer.hikari.{HikariDataSource, HikariConfig}
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.NamingStrategy
import io.getquill.JdbcContext
import io.getquill._
import maiden.config.MaidenConfig

trait MaidenBaseDB {

  def createDataSource = {
    val props = new Properties
    props.setProperty("dataSourceClassName",
                      MaidenConfig.get[String]("db.dataSourceClassName"))
    props.setProperty("dataSource.user",
                      MaidenConfig.get[String]("db.dataSource.user"))
    props.setProperty("dataSource.password",
                      MaidenConfig.get[String]("db.dataSource.password"))
    props.setProperty("dataSource.databaseName",
                      MaidenConfig.get[String]("db.dataSource.databaseName"))
    props.setProperty("dataSource.portNumber",
                      MaidenConfig.get[String]("db.dataSource.portNumber"))
    props.setProperty("dataSource.serverName",
                      MaidenConfig.get[String]("db.dataSource.serverName"))
    props.setProperty("connectionTimeout",
                      MaidenConfig.get[String]("db.connectionTimeout"))
    props.put("dataSource.logWriter", new PrintWriter(System.out));
    val config = new HikariConfig(props);
    new HikariDataSource(config);
  }
}

object DB extends MaidenBaseDB {
  val dbType = MaidenConfig.get[String]("migrations.database_type")

  lazy val db = if (dbType == "postgres") {
    PostgresDB.db
  } else {
    MySqlDB.db
  }

}

object MySqlDB extends MaidenBaseDB {

  val db = {
    val dbCasing = MaidenConfig.getOption[String]("migrations.database_casing") match {
      case Some(x) => x
      case _ => "snake_case"
    }

    dbCasing match {
      case ("snake_case") =>
        new JdbcContext[MySQLDialect, SnakeCase](createDataSource)
      case ("literal") =>
        new JdbcContext[MySQLDialect, Literal](createDataSource)
      case ("escape") =>
        new JdbcContext[MySQLDialect, Escape](createDataSource)
      case _ =>
        new JdbcContext[MySQLDialect, Escape](createDataSource)
    }
  }



}

object PostgresDB extends MaidenBaseDB {

  val db = {
    val dbType = MaidenConfig.get[String]("migrations.database_type")
    val dbCasing = MaidenConfig.getOption[String]("migrations.database_casing") match {
      case Some(x) => x
      case _ => "snake_case"
    }

    dbCasing match {
      case ("snake_case") =>
        new JdbcContext[PostgresDialect, SnakeCase](createDataSource)
      case ("literal") =>
        new JdbcContext[PostgresDialect, Literal](createDataSource)
      case ("escape") =>
        new JdbcContext[PostgresDialect, Escape](createDataSource)
      case _ =>
        new JdbcContext[PostgresDialect, Escape](createDataSource)
    }
  }



}
