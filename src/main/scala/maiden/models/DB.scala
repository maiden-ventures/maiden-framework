package maiden.models

import java.util.Properties
import java.io.PrintWriter
import scala.language.existentials
import com.zaxxer.hikari.{HikariDataSource, HikariConfig}
import io.getquill._
import maiden.config.MaidenConfig
import maiden.implicits._

trait MaidenBaseDB {

  def createDataSource = {
    println("create datasource")

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
    config.addDataSourceProperty("cachePrepStmts", "true");
    config.addDataSourceProperty("prepStmtCacheSize", "500");
    config.addDataSourceProperty("prepStmtCacheSqlLimit", "4096");
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
        new JdbcContext[MySQLDialect, SnakeCase](createDataSource) with DBImplicits with DateImplicits
      case ("literal") =>
        new JdbcContext[MySQLDialect, Literal](createDataSource) with DBImplicits with DateImplicits
      case ("escape") =>
        new JdbcContext[MySQLDialect, Escape](createDataSource) with DBImplicits with DateImplicits
      case _ =>
        new JdbcContext[MySQLDialect, Escape](createDataSource) with DBImplicits with DateImplicits
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
        new JdbcContext[PostgresDialect, SnakeCase](createDataSource) with DBImplicits with DateImplicits
      case ("literal") =>
        new JdbcContext[PostgresDialect, Literal](createDataSource) with DBImplicits with DateImplicits
      case ("escape") =>
        new JdbcContext[PostgresDialect, Escape](createDataSource) with DBImplicits with DateImplicits
      case _ =>
        new JdbcContext[PostgresDialect, Escape](createDataSource) with DBImplicits with DateImplicits
    }
  }



}
