package maiden.models

import java.util.Properties
import java.io.PrintWriter
import scala.language.existentials
import cats.data.Xor
import com.zaxxer.hikari.{HikariDataSource, HikariConfig}
import io.getquill._
import io.getquill.context.sql.idiom.SqlIdiom
import maiden.config.MaidenConfig
import maiden.implicits._
import maiden.types._
import maiden.types.MaidenResultTypes._


trait MaidenBaseDB {

  val dbType = MaidenConfig.get[String]("migrations.database_type")

  def createDataSource: DatasourceResult[HikariDataSource] = try {
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
    if (dbType == "mysql") {
      config.addDataSourceProperty("cachePrepStmts", "true");
      config.addDataSourceProperty("prepStmtCacheSize", "500");
      config.addDataSourceProperty("prepStmtCacheSqlLimit", "4096");
    }
    Xor.right(DatasourceValue(new HikariDataSource(config)));
  } catch {
    case e: Exception => Xor.left(DatasourceError("Unable to create datasource", Option(e)))
  }

  val dbCasing =
    MaidenConfig.getOption[String]("migrations.database_casing") match {
      case Some(x) => x
      case _ => "snake_case"
    }

  val datasource = createDataSource match {
    case Xor.Left(_) => throw(new Exception("unable to createDataSource"))
    case Xor.Right(ds) => ds.unwrapped
  }
}

object DB extends MaidenBaseDB {

  lazy val db = if (dbType == "postgres") {
    PostgresDB.db
  } else {
    MySqlDB.db
  }

}

class  MaidenDbContext[I <: SqlIdiom, N <: NamingStrategy](ds: HikariDataSource)
    extends JdbcContext[I,N](ds)
    with DBImplicits with DateImplicits

object MySqlDB extends MaidenBaseDB {
  val db =
    dbCasing match {
      case ("snake_case") =>
        new MaidenDbContext[MySQLDialect, SnakeCase](datasource)
      case ("literal") =>
        new MaidenDbContext[MySQLDialect, Literal](datasource)
      case ("escape") =>
        new MaidenDbContext[MySQLDialect, Escape](datasource)
      case _ =>
        new MaidenDbContext[MySQLDialect, Escape](datasource)
    }
}


object PostgresDB extends MaidenBaseDB {
  val db =
    dbCasing match {
      case ("snake_case") =>
        new MaidenDbContext[PostgresDialect, SnakeCase](datasource)
      case ("literal") =>
        new MaidenDbContext[PostgresDialect, Literal](datasource)
      case ("escape") =>
        new MaidenDbContext[PostgresDialect, Escape](datasource)
      case _ =>
        new MaidenDbContext[PostgresDialect, Escape](datasource)
    }
}
