package maiden.models

import java.util.Properties
import java.io.PrintWriter
import com.zaxxer.hikari.{HikariDataSource, HikariConfig}
import io.getquill._
//import io.getquill.SnakeCase
//import io.getquill.{MySQLDialect, PostgresDialect}
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
  val db =
    if (MaidenConfig.get[String]("migrations.database_type") == "postgresql")
      PostgresDB.db
    else
      MySqlDB.db

}
object PostgresDB extends MaidenBaseDB {
  //val db = new PostgresAsyncContext[SnakeCase]("db")

  final val db = new JdbcContext[PostgresDialect, SnakeCase](createDataSource)

}

object MySqlDB extends MaidenBaseDB {

  lazy val db = new JdbcContext[MySQLDialect, SnakeCase](createDataSource)

}
