package maiden.annotations

import scala.annotation._

trait MaidenAnnotation extends StaticAnnotation

//for DB Annotations
trait MaidenDbAnnotation extends MaidenAnnotation
//for API Annotations
trait MaidenApiAnnotation extends MaidenAnnotation

case class DBType(typeName: String) extends MaidenDbAnnotation
case class DBAutoIncrement() extends MaidenDbAnnotation
case class DBName(name: String) extends MaidenDbAnnotation
case class DBNullable() extends MaidenDbAnnotation
case class DBUnique() extends MaidenDbAnnotation
case class DBDefault(expr: String) extends MaidenDbAnnotation
case class DBLimit(length: Int) extends MaidenDbAnnotation
case class DBPrimaryKey() extends MaidenDbAnnotation
case class DBIndex() extends MaidenDbAnnotation
case class DBForeignKey(referenceTable: String, referenceColumn: String,
                        onDeleteAction: String = "cascade") extends MaidenDbAnnotation



case class RequiresAuth() extends MaidenApiAnnotation
