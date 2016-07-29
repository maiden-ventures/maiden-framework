package maiden.models

import java.util.Properties
import java.io.PrintWriter
import com.zaxxer.hikari.{HikariDataSource, HikariConfig}
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

  type PG = PostgresDialect
  type MY = MySQLDialect

  type PGSC = JdbcContext[PG, SnakeCase]
  type MYSC = JdbcContext[MY, SnakeCase]
  type PGL = JdbcContext[PG, Literal]
  type MYL = JdbcContext[MY, Literal]
  type PGE = JdbcContext[PG, Escape]
  type MYE = JdbcContext[MY, Escape]

  val db = {
    val dbType = MaidenConfig.get[String]("migrations.database_type")
    val dbCasing = MaidenConfig.getOption[String]("migrations.database_casing") match {
      case Some(x) => x
      case _ => "snake_case"
    }

    (dbType, dbCasing) match {
      case ("postgres", "snake_case") => new PGSC(createDataSource)
      case ("mysql", "snake_case") => new MYSC(createDataSource)
      case ("postgres", "literal") => new PGL(createDataSource)
      case ("mysql", "literal") => new MYL(createDataSource)
      case ("postgres", "escape") => new PGE(createDataSource)
      case ("mysql", "escape") => new MYE(createDataSource)
    }
  }
}
