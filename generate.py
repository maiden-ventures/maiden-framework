import inflection
from yaml import load, dump
try:
    from yaml import CLoader as Loader, CDumper as Dumper
except ImportError:
    from yaml import Loader, Dumper

import sys
import os

from datetime import datetime
from subprocess import call

def existsAndTrue(data, key):
    if key in data and data[key]: return True
    else: return False


def format_scala(directory):
    call(["java", "-jar", "./tools/scalafmt.jar", "-i", "-f", directory])


class MigrationBuilder:


  def __init__(self, data, config):
    self.data = data
    self.config = config
    self.template = """
    package %s.models

    import com.imageworks.migration._

    class %s extends Migration {
      val table = "%s"

      def up() = {
        createTable(table) { t =>
          %s
        }
      }

      %s

      %s

      def down() = {
        dropTable(table)
      }
    }

    """

    self.build()


  def buildColumn(self, table, info):
      modifiers = ["NotNull"]

      if 'db_name' not in info: info['db_name'] = info['name']
      if existsAndTrue(info, 'auto_increment'): modifiers.append("AutoIncrement")
      if existsAndTrue(info, 'primary_key'): modifiers.append("PrimaryKey")
      if existsAndTrue(info, 'nullable'): modifiers.remove("NotNull")
      if 'default' in info: modifiers.append("""Default("%s")""" % (info['default']))
      if 'limit' in info: modifiers.append("""Limit(%s)""" % (info['limit']))
      #if 'db_name' in info: info['db_name = info['db_name']

      modifiers = ', '.join(modifiers)

      self.columns.append("""t.%s("%s", %s)""" % (info['type'], info['db_name'], modifiers))

      if 'index' in info:
          index_name = "%s_%s_index"  % (table, info['db_name'])
          self.indexes.append("""addIndex(table, Array("%s"), Name("%s"))""" % (info['db_name'], index_name))

      if 'references' in info:
          ref = info['references']
          ref_table = ref['table']
          ref_column= ref['column']
          ref_name = "fk_%s_%s" % (ref_table, ref_column)

          on_delete = "Ignore"
          if 'on_delete' in ref:
              on_delete = ref['on_delete']

          s = """addForeignKey(on("%s" -> "%s"),
          references("%s" -> "%s"),
          OnDelete(%s),
          Name("%s"))""" % (ref_table, ref_column, table, info['db_name'], on_delete, ref_name)

          self.references.append(s)


  def build(self):
    migration_dir = os.path.abspath(os.path.join(self.config["app"]["source_directory"], "src/main/scala", "%s/migrations" % (self.config["app"]["name"])))

    #make sure the models directorty exists
    if not os.path.exists(migration_dir):
        os.makedirs(migration_dir)

    for model in self.data:
        if 'db_name' not in model:
            model['db_name'] = inflection.underscore(model['name'])

        self.columns = []
        self.indexes = []
        self.references = []

        now_ts = str(datetime.now()).replace("-","").replace(":","").replace(".","").replace(" ", "")

        migration_name = "Create%s" % (inflection.camelize(model["name"]))
        class_name = "Migrate_%s_%s"  % (now_ts, migration_name)
        table_name = model["name"]
        file_name = "%s.scala" % (class_name.replace("Migrate_", ""))

        for col in model['columns']:
            self.buildColumn(model['db_name'], col)


        package = "%s.%s" % (self.config["app"]["namespace"], self.config["app"]["name"])
        column_str = "\n".join(self.columns)
        indexes_str = "\n".join(self.indexes)
        references_str = "\n".join(self.references)
        table_str = model['db_name']

        out = self.template % (package, class_name, table_str, column_str, indexes_str, references_str)
        fd = open(os.path.join(migration_dir, file_name), "w+")
        fd.write(out)
        fd.close()
    format_scala(migration_dir)




class SbtBuilder:

    def __init__(self, config):

        self.config = config
        if not os.path.exists(self.config["app"]["source_directory"]):
            os.makedirs(self.config["app"]["source_directory"])

        if not os.path.exists(os.path.join(self.config["app"]["source_directory"], "project")):
            os.makedirs(os.path.join(self.config["app"]["source_directory"], "project"))

        self.sbt_template = open("./code-templates/sbt").read()
        self.plugins_template =  open("./code-templates/sbt-plugins").read()

        self.build_sbt()
        self.build_plugins()


    def build_sbt(self):
        self.sbt_template = self.sbt_template.replace("@@namespace@@", self.config["app"]["namespace"]).replace("@@appName@@", self.config["app"]["name"])
        fd = open(os.path.join(self.config["app"]["source_directory"], "build.sbt"), "w+")
        fd.write(self.sbt_template)
        fd.close()



    def build_plugins(self):
        fd = open(os.path.join(self.config["app"]["source_directory"], "project", "plugins.sbt"), "w+")
        fd.write(self.plugins_template)
        fd.close()


class DbAccessBuilder:

    def __init__(self, config):
        self.config = config
        self.build()

    def build(self):
        db_file = os.path.abspath(os.path.join(self.config["app"]["source_directory"], "src/main/scala", "%s/models/DB.scala" % (self.config["app"]["name"])))
        s = """package %s.%s.models
        import io.getquill._
import io.getquill.naming.SnakeCase
import io.getquill.sources.sql.idiom.PostgresDialect

object DB {
        lazy val db = source(new JdbcSourceConfig[PostgresDialect, SnakeCase]("db"))
        }""" % (self.config["app"]["namespace"], self.config["app"]["name"])

        fd = open(db_file, "w+")
        fd.write(s)
        fd.close()

class ModelBuilder:

  def __init__(self, data, config):
    self.data = data
    self.config = config
    self.prelude = """
  package %s.%s.models

  import java.time.LocalDateTime
  import io.getquill._
  import io.getquill.sources.sql.ops._
  import maiden.implicits.DateImplicits._
  import maiden.traits._
  import DB._

  """ % (config["app"]["namespace"], config["app"]["name"])

    self.DB_TO_SCALA = {
      "varchar": "String",
      "bigint": "Long",
      "int": "Int",
      "datetime": "LocalDateTime",
      "timestamp": "LocalDateTime"
     }

    self.build()

  def buildFindBy(self, model_name, field_name, field_type):
      s = """def findBy%s(value: %s) = {
      val q = quote {
        query[%s].filter(c => c.%s == lift(value))
      }
      db.run(q)
    }""" % (inflection.camelize(field_name), self.DB_TO_SCALA[field_type], inflection.camelize(model_name), inflection.camelize(field_name, False))

      return s

  def buildLikes(self, model_name, field_name):
      #only valid for varchar columns so we don't need a type
      s = """def findByLike%s(value: String) = {
      val likeQuery = s"%%${value}%%"
      val q = quote {
        query[%s].filter(c => c.%s like lift(likeQuery))
      }
      db.run(q)
    }""" % (inflection.camelize(field_name), inflection.camelize(model_name), inflection.camelize(field_name, False))

      return s

  def buildTimestampRange(self, model_name, field_name):
      s = """def findBy%sRange(start: LocalDateTime, end: LocalDateTime) = {
      val q = quote {
        query[%s].filter(c => c.%s > lift(start) && c.%s < lift(end) )
      }
      db.run(q)
    }""" % (inflection.camelize(field_name), inflection.camelize(model_name), inflection.camelize(field_name, False), inflection.camelize(field_name, False))

      return s

  def buildUpdate(self, model_name, column_names):
      pass

  def buildDeleteBy(self, model_name, field_name, field_type):
      s= """def deleteBy%s(value: %s) = {
      val a = quote {
        query[%s].filter(p => p.%s == lift(value)).delete
      }
      db.run(a)
    }""" % (inflection.camelize(field_name), self.DB_TO_SCALA[field_type], inflection.camelize(model_name), inflection.camelize(field_name, False))
      return s


  def buildReferences(self, model_name, field_name, model2_name, field2_name):
      #grab a foreign key reference
      table1 = inflection.camelize(model_name)
      table2 = inflection.camelize(model2_name)
      field1 = inflection.camelize(field_name)
      field2 = inflection.camelize(field2_name)

      s = """def get%s() = {
      val q = quote {
        query[%s].join(query[%s]).on((q1,q2) => q1.%s == q2.%s)
      }
      db.run(q)
    }""" % (table1, table1, table2, inflection.camelize(field1, False), inflection.camelize(field2, False))
      return s

  def build(self):

    models_dir = os.path.abspath(os.path.join(self.config["app"]["source_directory"], "src/main/scala", "%s/models" % (self.config["app"]["name"])))

    #make sure the models directorty exists
    if not os.path.exists(models_dir):
        os.makedirs(models_dir)


    for model in self.data:
        model_path= os.path.join(models_dir, "%s.scala" % (inflection.camelize(model["name"])))

        add_timestamps = False
        case_class = ""
        companion = ""

        if 'timestamps' in model:
            add_timestamps = True

        if 'db_name' not in model:
            model['db_name'] = model['name']

        case_class = "%s\ncase class %s (" % (case_class, model["name"])
        companion = "\nobject %s {" % (model["name"])

        columns = []
        for col in model["columns"]:

            modifiers = []
            col_name = inflection.camelize(col["name"], False)
            col_str = "  %s: %s" % (col_name, self.DB_TO_SCALA[col['type']])

            columns.append("\n%s" % (col_str))
        if 'timestamps' in model:
            columns.append("""  createdAt: LocalDateTime = LocalDateTime.now()""")
            columns.append("""  updatedAt: LocalDateTime = LocalDateTime.now()""")
        modifiers = []
        case_class = "%s\n%s\n) extends MaidenModel with WithApi" % ( case_class, ",  \n".join(columns))
        #print case_class
        #now build the object which contains our magic queries
        magic_methods = []
        for col in model["columns"]:
            magic_methods.append(self.buildFindBy(model["name"], col["name"], col['type']))
            magic_methods.append(self.buildDeleteBy(model["name"], col["name"], col["type"]))
            if 'references' in col:
                ref = col['references']
                magic_methods.append(self.buildReferences(ref["table"], ref["column"], model['name'], col['name']))
                #TODO: and now the reverse reference



        if 'timestamps' in model:
            magic_methods.append(self.buildFindBy(model["name"], "created_at", "timestamp"))
            magic_methods.append(self.buildFindBy(model["name"], "updated_at", "timestamp"))
            magic_methods.append(self.buildDeleteBy(model["name"], "created_at", "timestamp"))
            magic_methods.append(self.buildDeleteBy(model["name"], "updated_at", "timestamp"))

        like_columns = [x for x in model['columns'] if x['type'] == "varchar"]
        for c in like_columns:
            magic_methods.append(self.buildLikes(model['name'], c['name']))

        if 'timestamps' in model:
            magic_methods.append(self.buildTimestampRange(model['name'], 'created_at'))
            magic_methods.append(self.buildTimestampRange(model['name'], 'updated_at'))

        companion = "%s\n  %s\n}" % (companion, "\n\n  ".join(magic_methods))

        out = "%s\n%s\n%s" % (self.prelude, case_class, companion)

        fd = open(model_path, "w+")
        fd.write(out)
        fd.close()
    format_scala(models_dir)


class EncoderBuilder:

    def __init__(self, models, config):
        self.template = open("./code-templates/encoder").read()

        self.config = config
        self.models = models

        self.encoder_dir = os.path.join(self.config['app']['source_directory'], "src/main/scala/%s" % (self.config['app']['name']), "encoders")

        if not os.path.exists(self.encoder_dir):
            os.makedirs(self.encoder_dir)
            self.build()

    def build(self):
        if 'payload_key' in self.config['app']:
          payload_key = self.config['app']['payload_key']
        else:
            payload_key = ""

        package_name = "%s.%s" % (self.config['app']['namespace'], self.config['app']['name'])

        for model in self.models:
            model_name = inflection.camelize(model['name'])
            lower_model_name = inflection.camelize(model['name'], False)
            fields = [x['name'] for x in model['columns']]

            json_fields = ['"%s" -> m.%s' % (inflection.underscore(x), inflection.camelize(x, False)) for x in fields]

            json_fields_str = ", ".join(json_fields)

            out = self.template.replace("@@package@@", package_name).replace("@@model@@", model_name).replace("@@lowerCaseModel@@", lower_model_name).replace("@@jsonFields@@", json_fields_str).replace("@@payloadKey@@", payload_key)

            file_name = os.path.join(self.encoder_dir, "%s.scala" % (model_name))

            fd = open(file_name, "w+")
            fd.write(out)
            fd.close()
        format_scala(self.encoder_dir)


if __name__ == "__main__":
    #read in our configs
    model_stream = open("models.yml")
    app_stream = open("project.yml")

    app_data = load(app_stream, Loader=Loader)
    model_data = load(model_stream, Loader=Loader)
    sbt_builder = SbtBuilder(app_data)
    model_builder = ModelBuilder(model_data, app_data)
    access_builder =  DbAccessBuilder(app_data)
    migration_builder = MigrationBuilder(model_data, app_data)
    encoder_builder = EncoderBuilder(model_data, app_data)
