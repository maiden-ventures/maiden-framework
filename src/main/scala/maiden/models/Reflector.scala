package maiden.models

import java.io.File
import java.nio.file.{Files, Paths}
import java.net.URLClassLoader
import java.lang.reflect.Constructor
import org.joda.time.format.ISODateTimeFormat
import scala.reflect._
import scala.reflect.runtime.universe._
import scala.reflect.runtime.{universe => ru}
import scala.collection.immutable.ListMap
import scala.collection.JavaConverters._
import com.thoughtworks.paranamer._
import org.clapper.classutil.ClassFinder
import org.joda.time._
import com.twitter.util.Eval
import maiden.traits._
import maiden.annotations._
import maiden.common.Text._
import maiden.common.{FileReader, FileWriter}
//import scala.annotation._
import java.sql.Timestamp

@DBName("test")
case class X(
  @DBPrimaryKey() @DBName("fred") @DBDefault("100")
  id: Long,
  @DBName("stupid_name")
  @DBForeignKey("fucker", "name", "Cascade")
  name: String,
  @DBIndex()
  @DBLimit(100)
  @DBNullable()
    abc: String,
  zzz: Timestamp) extends MaidenModel
case class Y(i: Long, n: String, a: String) extends MaidenModel

case class ReflectionConfig(appName: String = "app",
                          namespace: String = "appnamespace",
                          sourceDirectory: String = ".",
                          keepUserCode: Boolean = true,
                          generateMigrations: Boolean = false,
                          generateTests: Boolean = false,
                          //whether to generate `findByX` methods
                          generateMagicMethods: Boolean = true,
                          payloadKey: String = "payload")


case class ColumnInfo(var name: String, annotations: List[Annotation],
                      columnType: scala.reflect.runtime.universe.Type)


case class ModelInfo(var name: String, annotations: List[Annotation],
                     columns: List[ColumnInfo])

object Reflector {
  type ReflectType = scala.reflect.runtime.universe.Type


  final val MODEL_TRAIT = "maiden.traits.MaidenModel"
  final val TIMESTAMP_TRAIT = "maiden.traits.WithTimestamps"
  final val API_TRAIT = "maiden.traits.WithApi"
  final val ADMIN_TRAIT = "maiden.traits.WithAdmin"
  final val AUTH_TRAIT = "maiden.traits.WithAuthorization"

  final val TRAIT_MAP = Map(
    "models" -> MODEL_TRAIT,
    "api" -> API_TRAIT,
    "admin" -> ADMIN_TRAIT,
    "auth" -> AUTH_TRAIT
  )

  private[this] def getClassName(fullPath: String) = if (fullPath.contains('.')) fullPath.split('.').last else fullPath

  def main(args: Array[String]): Unit = {
    //build the reflection config
    val parser = new scopt.OptionParser[ReflectionConfig]("maiden") {
      head("Maiden Source Generator", "1.0")

      opt[String]('a', "app-name").action( (x, c) =>
        c.copy(appName = x)).text("The name of the app [MyApi]")

      opt[String]('n', "namespace").action( (x, c) =>
        c.copy(namespace = x)).text("The namespace for the app [com.company]")

      opt[String]('s', "source-directory").action( (x, c) =>
        c.copy(sourceDirectory = x)).text("Where to place generated files [.]")

      opt[Unit]('m', "generate-migrations").action( (_, c) =>
        c.copy(generateMigrations = true)).text("Whether to generate migrations")

      opt[Unit]('t', "generate-tests").action( (_, c) =>
        c.copy(generateTests = true)).text("Whether to generate tests")

      opt[Unit]('c', "generate-magic-methods").action( (_, c) =>
        c.copy(generateMagicMethods = true)).text("Whether to generate magic methods")

      opt[String]('p', "payload-key").action( (x, c) =>
        c.copy(payloadKey = x)).text("The key to use for JSON payloads")

      help("help").text("prints this usage text")
    }

    parser.parse(args, ReflectionConfig()) match {
      case Some(config) => {
        val maidenClasses = getMaidenClasses
        maidenClasses("models").foreach(model => {
          val name = getClassName(model._1.toString)
          val modelInfo = model._2
          buildMigration(modelInfo, config)
          Thread.sleep(2)
        })


      }

      case None => ()
    }

  }



  def getMaidenClasses() = {
    val classpath = List(".").map(new File(_))
    val finder = ClassFinder(classpath)
    val classes = finder.getClasses
    val classMap = ClassFinder.classInfoMap(classes)

    TRAIT_MAP.map { case (name, traitName) => {
      name -> {
        val plugins = ClassFinder.concreteSubclasses(traitName, classMap).toList
        plugins.map(x => getClassName(x.toString) -> getModelInfo(x.toString)).toMap
      }
    }}.toMap
  }

  def getModelInfo(className:  String)/*: ListMap[String, Type]*/ = {
    val cls = Class.forName(className)
    val tpe = ru.runtimeMirror(cls.getClassLoader).classSymbol(cls).toType
    val constructorSymbol = tpe.decl(termNames.CONSTRUCTOR)
    val defaultConstructor =
      if (constructorSymbol.isMethod) constructorSymbol.asMethod
      else {
        val ctors = constructorSymbol.asTerm.alternatives
        ctors.map { _.asMethod }.find { _.isPrimaryConstructor }.get
      }

    val modelClass: ClassSymbol = ru.runtimeMirror(Thread.currentThread().getContextClassLoader).staticClass(className)
    val modelAnnotations = modelClass.annotations
    val columns = defaultConstructor.paramLists.reduceLeft(_ ++ _).map { sym =>
      ColumnInfo(name = sym.name.toString,
        annotations = sym.annotations.toList,
        columnType = tpe.member(sym.name).asMethod.returnType)
    }.toList

    ModelInfo(name = getClassName(className),
      columns = columns,
      annotations = modelAnnotations)
  }

  private[this] def writeFile(source: String, fileName: String) = {
    val formatted = org.scalafmt.Scalafmt.format(source).get
    FileWriter.write(formatted, fileName)
  }

  def buildMigration(info: ModelInfo, rc: ReflectionConfig) = {

    def _createColumn(c: ColumnInfo, modifiers: String) = {
      val dbType = DBMapper.scalaTypeToDBType(c.columnType.toString)
      s"""\tt.${dbType}("${c.name}", ${modifiers})"""
    }

    val migrationPath = s"${rc.sourceDirectory}/src/main/scala/migrations"
    new File(migrationPath).mkdirs()
    val fmt = ISODateTimeFormat.dateHourMinuteSecondMillis
    val date = fmt.print(System.currentTimeMillis)
      .replace(":", "")
      .replace("-", "")
      .replace("T", "")
      .replace(".", "")

    val migrationName = s"Create${info.name}"
    val className = s"Migrate_${date}_${pascalize(migrationName)}"
    val file = s"${migrationPath}/${className.replace("Migrate_", "")}.scala"
    var tableName = underscore(info.name)

    var indexes = scala.collection.mutable.ListBuffer[String]()
    var foreignKeys = scala.collection.mutable.ListBuffer[String]()
    val columns = info.columns.map{ c =>
      var columnName = c.name
      val initialModifiers = scala.collection.mutable.ListBuffer("NotNull")
      val modifiers = c.annotations.map {m => {
        val e = new Eval(None)
        //handle modifiers that go  directly into the column declaration
        e.apply[Any](m.toString.replace("$", "")) match {
          case DBPrimaryKey => "PrimaryKey"
          case DBUnique =>"Unique"
          case DBAutoIncrement => "AutoIncrement"
          case DBDefault(expr) => s"""Default("${expr}")"""
          case DBLimit(limit) => s"""Limit(${limit})"""
          case DBNullable => {initialModifiers -= "NotNull"; null}
          case DBName(name) => {c.name = name; null}
          case DBIndex => {
            indexes += s"""addIndex(table, Array("${columnName}"), Name("${tableName}_${columnName}_index"))"""
            null
          }
          case DBForeignKey(refTable, refColumn, onDelete) => {
            val fk =
              s"""addForeignKey(on("${refTable}" -> "${refColumn}"),
                references("${tableName}" -> "${columnName}"),
                OnDelete(${onDelete}),
                Name("fk_${refTable}_${refColumn}")
              )
             """.stripMargin
            foreignKeys += fk
            null
          }
          case _ => null
        }
      }}.filter(x => x != null)
      val finalModifiers = (initialModifiers.toList ++ modifiers).mkString(",")
      _createColumn(c, finalModifiers)
      //_createColumn(c).mkString("\n")
    }
    val sql = FileReader.read("./code-templates/migration")
    val out = sql
      .replaceAll("@@package@@", s"${rc.namespace}.${rc.appName}.migrations")
      .replaceAll("@@className@@", className)
      .replaceAll("@@tableName@@", tableName)
      .replaceAll("@@columns@@", columns.mkString("\n"))
      .replaceAll("@@indexes@@", indexes.toList.mkString("\n"))
      .replaceAll("@@foreignKeys@@", foreignKeys.toList.mkString("\n"))


    writeFile(out, file)
    //println(columns.mkString(", \n"))
    //println(indexes.toList.mkString("\n"))
    //println(foreignKeys.toList.mkString("\n"))
  }

  def buildApi(info: ModelInfo) = {

  }

  def buildAdmin(info: ModelInfo) = {

  }

}
