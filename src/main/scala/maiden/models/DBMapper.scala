package maiden.models


object DBMapper {

  val scalaTypeToDBType = Map(
    "scala.Long" -> "bigint",
    "scala.Int" -> "int",
    "String" ->  "varchar",
    "java.sql.Timestamp" -> "datetime",

    //not currently used..auto serializing types
    "Map[String,scala.Any]" -> "text",
    "List[scala.Any]" -> "text"
  )
}
