import inflection
from yaml import load, dump
try:
    from yaml import CLoader as Loader, CDumper as Dumper
except ImportError:
    from yaml import Loader, Dumper

import sys
import os
import time

from datetime import datetime
import subprocess

SCALA_FILES = []

DB_TO_SCALA = {
    "varchar": "String",
    "bigint": "Long",
    "int": "Int",
    "datetime": "LocalDateTime",
    "timestamp": "LocalDateTime"
  }

def exists_and_true(data, key):
    if key in data and data[key]: return True
    else: return False


def key_with_default(data, key, default):
    if key in data and data[key]: return data[key]
    if key not in data: return default

def read_template(name):
    fd = open(os.path.join("./code-templates", name))
    contents = fd.read()
    fd.close()
    return contents

def format_scala():
    for d in SCALA_FILES:
        subprocess.call(["java", "-jar", "./tools/scalafmt.jar", "-i", "-s", "default", "--alignByOpenParenCallSite", "true", "--alignByArrowEnumeratorGenerator", "true", "--maxColumn", "80", "-f", d], stdout=subprocess.PIPE)


class MigrationBuilder:

  def __init__(self, data, config):
    self.data = data
    self.config = config
    self.template = """
    package %s.migrations

    import com.imageworks.migration._

    class %s extends Migration {
      val table = "%s"

      def up() = {
        createTable(table) { t =>
          %s
        }

        %s

        %s
      }

      def down() = {
        dropTable(table)
      }
    }

    """

    self.build()


  def buildColumn(self, table, info):
      modifiers = ["NotNull"]

      if 'db_name' not in info: info['db_name'] = info['name']
      if exists_and_true(info, 'auto_increment'): modifiers.append("AutoIncrement")
      if exists_and_true(info, 'primary_key'): modifiers.append("PrimaryKey")
      if exists_and_true(info, 'nullable'): modifiers.remove("NotNull")
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
          Name("%s"))""" % (ref_table, ref_column, inflection.underscore(table).lower(), info['db_name'], on_delete, ref_name)

          self.references.append(s)


  def build(self):
    migration_dir = os.path.abspath(os.path.join(self.config["base_path"], "migrations"))

    #make sure the models directorty exists
    if not os.path.exists(migration_dir):
        os.makedirs(migration_dir)

    for model in self.data:
        if 'db_name' not in model:
            model['db_name'] = inflection.underscore(model['name'])

        self.columns = []
        self.indexes = []
        self.references = []

        now_ts = str(datetime.now()).split(".")[0].replace("-","").replace(":","").replace(".","").replace(" ", "")

        migration_name = "Create%s" % (inflection.camelize(model["name"]))
        class_name = "Migrate_%s_%s"  % (now_ts, migration_name)
        table_name = inflection.underscore(model["name"]).lower()
        file_name = "%s.scala" % (class_name.replace("Migrate_", ""))

        for col in model['columns']:
            self.buildColumn(model['db_name'], col)


        package = self.config['package']
        column_str = "\n".join(self.columns)
        indexes_str = "\n".join(self.indexes)
        references_str = "\n".join(self.references)
        table_str = model['db_name']

        out = self.template % (package, class_name, table_str, column_str, indexes_str, references_str)
        fd = open(os.path.join(migration_dir, file_name), "w+")
        fd.write(out)
        fd.close()
        time.sleep(2)
    SCALA_FILES.append(migration_dir)




class SbtBuilder:

    def __init__(self, config):

        self.config = config
        if not os.path.exists(self.config["app"]["source_directory"]):
            os.makedirs(self.config["app"]["source_directory"])

        if not os.path.exists(os.path.join(self.config["app"]["source_directory"], "project")):
            os.makedirs(os.path.join(self.config["app"]["source_directory"], "project"))

        self.sbt_template = read_template("sbt")
        self.plugins_template =  read_template("sbt-plugins")

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

    def __init__(self, models, config):
        self.config = config
        self.models = models
        self.build()

    def build(self):
        db_file = read_template("db-driver")
        file_name = os.path.join(self.config["base_path"], "models/DB.scala")
        db = self.config['db']
        db_type = db['driver']
        db_name = db['name']
        db_user = db['user']
        db_password = db['password']
        db_host = db.get("host", "localhost")

        default_port = ""
        dialect_name = ""
        template = "postgres-props"
        if db_type == "postgres":
            default_port = "5432"
            dialect_name = "Postgres"
            template = "postgres-props"

        if db_type == "mysql":
            default_port = "3336"
            dialect_name = "MySQL"
            template = "mysql-props"

        db_port = db.get("port", default_port)

        #write out DB file
        driver = db_file.replace("@@package@@", self.config["package"]).replace("@@dbDialect@@", dialect_name)


        fd = open(file_name, "w+")
        fd.write(driver)
        fd.close()
        SCALA_FILES.append(file_name)

        template = read_template(template)
        props = template.replace("@@host@@", db_host).replace("@@port@@", db_port).replace("@@db@@", db_name).replace("@@user@@", db_user).replace("@@password@@", db_password).replace("@@package@@", self.config["package"]).replace("@@sourceDir@@", self.config["base_path"])

        #now write out the application.properties
        fd = open(os.path.join(self.config['config_path'], "application.properties"), "w+")
        fd.write(props)

        fd.close()

        schema = read_template("schema")
        schema_list = ["lazy val %sQuery = quote(query[%s])" % (inflection.camelize(m["name"], False), inflection.camelize(m["name"])) for m in self.models]

        schema_list_str = "\n".join(schema_list)

        out = schema.replace("@@package@@", self.config["package"]).replace("@@schemaList@@", schema_list_str).replace("@@appName@@", inflection.camelize(self.config["app"]["name"]))

        fd = open(os.path.join(self.config['base_path'], "models/Schema.scala"), "w+")
        fd.write(out)
        fd.close()

class ModelBuilder:

  def __init__(self, data, config):
    self.data = data
    self.config = config


    self.prelude = """
package %s.models

import java.time.LocalDateTime
import io.getquill._
import io.getquill.sources.sql.ops._
import maiden.implicits.DateImplicits._
import maiden.implicits.DBImplicits._
import maiden.traits._
import DB._

  """ % (config["package"])


    self.build()


  def buildExists(self):
      s = """def exists(id: Long) = {
        val q = quote {
          %s.filter(_.id == lift(id))
        }

        db.run(q).headOption match {
          case Some(x) => true
          case _ => false
        }
      }""" % (self.query_name)

      return s

  def buildFindBy(self, model_name, field_name, field_type):
      s = """def findBy%s(value: %s) = {
        db.run(%s.filter(c =>  c.%s == lift(value)))
    }""" % (inflection.camelize(field_name), DB_TO_SCALA[field_type], self.query_name, inflection.camelize(field_name, False))

      return s

  def buildFindByRange(self, model_name, field_name, field_type):
      s = """def getRangeBy%s(start: Int = 0, count: Int = 20) = {
        db.run(%s.sortBy(c =>  c.%s).drop(lift(start)).take(lift(count)))
    }""" % (inflection.camelize(field_name, True), self.query_name, inflection.camelize(field_name, False))

      return s


  def buildLikes(self, model_name, field_name):
      #only valid for varchar columns so we don't need a type
      s = """def findByLike%s(value: String) = {
      val likeQuery = s"%%${value}%%"
      db.run(%s.filter(c => c.%s like lift(likeQuery)))
    }""" % (inflection.camelize(field_name), self.query_name, inflection.camelize(field_name, False))

      return s

  def buildTimestampRange(self, model_name, field_name):
      s = """def findBy%sRange(start: LocalDateTime, end: LocalDateTime) = {
      val q = quote {
        (s: LocalDateTime, e: LocalDateTime) =>
          %s.filter(c => c.%s > s && c.%s < e)
      }
      db.run(q)(start, end)
    }""" % (inflection.camelize(field_name), self.query_name, inflection.camelize(field_name, False), inflection.camelize(field_name, False))

      return s

  def buildCreate(self, model_name, models):
      cols = [(inflection.camelize(c["name"], False), DB_TO_SCALA[c["type"]]) for c in models if c["name"] not in ("id", "created_at", "updated_at")]
      create_args = ", ".join(["%s: %s" % (c[0], c[1]) for c in cols])

      case_class_args = ", ".join(["%s = %s" % (c[0], c[0]) for c in cols])

      s = ""
      if self.config["db"]["driver"] == "postgres":
          s = """def create(%s) = {
          val q = quote {
            query[%s].schema(_.generated(_.id)).insert
          }
          db.run(q)(%s(%s))
          }"""
      s =  s % (create_args, inflection.camelize(model_name), inflection.camelize(model_name), case_class_args)

      return s

  def __buildUpdateMatcher(self, field_name):
      s = """%s match {
        case Some(v) => if (v != existing.%s) existing.%s = v
        case _ => ()
      }""" % (field_name, field_name, field_name)

      return s

  def buildUpdate(self, columns ):
      cols = [(inflection.camelize(c["name"], False), DB_TO_SCALA[c["type"]]) for c in columns if c["name"] not in ("id", "created_at", "updated_at")]
      params = ", ".join(["%s: Option[%s] = None" % (c[0], c[1]) for c in cols])

      matches = []
      for c in cols:
          matches.append(self.__buildUpdateMatcher(inflection.camelize(c[0], False)))

      matches =  "\n  ".join(matches)
      s = """def update(id: Long, %s) = {
        val existing = findById(id).head
        existing.updatedAt = LocalDateTime.now

        %s

        val q = quote {
          %s.filter(_.id == lift(id)).update
        }

        db.run(q)(existing)
        existing
      }""" % (params, matches, self.query_name)

      return s





  def buildDeleteBy(self, model_name, field_name, field_type):
      s= """def deleteBy%s(value: %s) = {
        db.run(%s.filter(p => p.%s == lift(value)).delete)
    }""" % (inflection.camelize(field_name), DB_TO_SCALA[field_type], self.query_name, inflection.camelize(field_name, False))
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

    models_dir = os.path.abspath(os.path.join(self.config["base_path"], "models"))

    #make sure the models directory exists
    if not os.path.exists(models_dir):
        os.makedirs(models_dir)


    for model in self.data:
        self.query_name = "%sQuery" % (inflection.camelize(model["name"], False))
        model_path= os.path.join(models_dir, "%s.scala" % (inflection.camelize(model["name"])))

        case_class = ""
        companion = ""

        if 'db_name' not in model:
            model['db_name'] = model['name']

        case_class = "%s\ncase class %s (" % (case_class, model["name"])
        companion = "\nobject %s {\n\n  import %sSchema._\n\n" % (model["name"], inflection.camelize(self.config["app"]["name"]))

        columns = []

        for col in model["columns"]:
          modifiers = []
          col_name = inflection.camelize(col["name"], False)
          if col_name == "id":
              col_str = "  id: Long = -1"
          elif col_name  == "createdAt":
              col_str = "  createdAt: LocalDateTime = LocalDateTime.now"
          elif col_name  == "updatedAt":
              col_str = "  var updatedAt: LocalDateTime = LocalDateTime.now"
          else:
              col_str = "  var %s: %s" % (col_name, DB_TO_SCALA[col['type']])

          columns.append("\n%s" % (col_str))
        modifiers = []
        case_class = "%s\n%s\n) extends MaidenModel with WithApi" % ( case_class, ",  \n".join(columns))
        #print case_class
        #now build the object which contains our magic queries
        magic_methods = []
        magic_methods.append(self.buildExists())
        for col in model["columns"]:
            magic_methods.append(self.buildFindBy(model["name"], col["name"], col['type']))
            magic_methods.append(self.buildFindByRange(model["name"], col["name"], col['type']))
            magic_methods.append(self.buildDeleteBy(model["name"], col["name"], col["type"]))
            if 'references' in col:
                ref = col['references']
                magic_methods.append(self.buildReferences(ref["table"], ref["column"], model['name'], col['name']))
                #TODO: and now the reverse reference

        like_columns = [x for x in model['columns'] if x['type'] == "varchar"]
        for c in like_columns:
            magic_methods.append(self.buildLikes(model['name'], c['name']))

        magic_methods.append(self.buildTimestampRange(model['name'], 'created_at'))
        magic_methods.append(self.buildTimestampRange(model['name'], 'updated_at'))

        magic_methods.append(self.buildCreate(model['name'], model['columns']))
        magic_methods.append(self.buildUpdate(model["columns"]))
        companion = "%s\n  %s\n}" % (companion, "\n\n  ".join(magic_methods))

        out = "%s\n%s\n%s" % (self.prelude, case_class, companion)

        fd = open(model_path, "w+")
        fd.write(out)
        fd.close()
    SCALA_FILES.append(models_dir)


class EncoderBuilder:

    def __init__(self, models, config):
        self.template = read_template("encoder")

        self.config = config
        self.models = models

        self.encoders = []

        self.encoder_dir = os.path.join(self.config['base_path'], "encoders")

        if not os.path.exists(self.encoder_dir):
            os.makedirs(self.encoder_dir)
            self.build()

    def build(self):
        if 'payload_key' in self.config['app']:
          payload_key = self.config['app']['payload_key']
        else:
            payload_key = ""

        package_name = self.config['package']

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
            self.encoders.append("%sResponseEncoders" % (model_name))

        SCALA_FILES.append(self.encoder_dir)

        #now write out the master response encoders file
        encoder_traits = ' '.join(["with %s" % (x) for x in self.encoders])
        enc = read_template("response-encoders")

        app_name = self.config['app']['name']
        package = self.config['package']
        out = enc.replace("@@package@@", package).replace("@@response_encoders@@", encoder_traits)
        file_name = os.path.join(self.config['base_path'], "ResponseEncoders.scala")
        fd = open(file_name, "w+")
        fd.write(out)
        fd.close()
        SCALA_FILES.append(file_name)

class ApiBuilder:

    def __init__(self, models, config):
        self.models = models
        self.config = config
        self.template = read_template("api")
        self.api_dir = os.path.join(self.config['base_path'], "api")

        if not os.path.exists(self.api_dir):
            os.makedirs(self.api_dir)

        self.build()

    def build(self):
        for model in self.models:
            param_str = """param("%s").as[%s]"""

            all_model_cols = ["%s: %s" % (inflection.camelize(c["name"], False), DB_TO_SCALA[c["type"]]) for c in model['columns']]
            all_model_cols_str = ", ".join(all_model_cols)

            cols = [(inflection.camelize(c["name"], False), DB_TO_SCALA[c["type"]]) for c in model['columns'] if c["name"] not in ("id", "created_at", "updated_at")]
            create_args = " :: ".join([param_str % (c[0], c[1]) for c in cols])
            create_param_args = " :: ".join(['param("%s")' % (c[0]) for c in cols])
            create_params = ", ".join(["%s: %s" % (c[0], c[1]) for c in cols])
            model_creation_args = ", ".join([c[0] for c in cols])

            optional_params = " :: ".join(['paramOption("%s")' % (c[0]) for c in cols])
            param_list = ", ".join(["%s: Option[%s]" % (c[0], c[1]) for c in cols])
            update_params = ", ".join([c[0] for c in cols])

            out = self.template.replace("@@model@@", inflection.camelize(model['name'], True)).replace("@@lowerCaseModel@@", inflection.camelize(model['name'], False)).replace("@@package@@", self.config['package']).replace("@@createArgs@@", create_args).replace("@@createParamArgs@@", create_param_args).replace("@@createParams@@", create_params).replace("@@modelCreationArgs@@", model_creation_args).replace("@@modelColumns@@", all_model_cols_str).replace("@@optionalParams@@", optional_params).replace("@@paramList@@", param_list).replace("@@updateParams@@", update_params)

            fd = open(os.path.join(self.api_dir, "%s.scala" % (model['name'])), "w+")
            fd.write(out)
            fd.close()
        SCALA_FILES.append(self.api_dir)

def build_boot(app_data):
    template = read_template("boot")
    app_name = app_data['app']['name']
    package = app_data['package']

    out = template.replace("@@package@@", package).replace("@@appNameUpper@@", inflection.camelize(app_name))

    file_name = os.path.join(app_data['base_path'], "App.scala")
    fd = open(file_name, "w+")
    fd.write(out)
    fd.close()

    SCALA_FILES.append(file_name)


def build_api_service(models, config):

    apis = []
    #add create, search, etc...
    endpoints = ['doc', 'get', 'delete']
    api_names = [m["name"] for m in models]
    for api in api_names:
        apis.append("%sApi.%sApi()" % (inflection.camelize(api), inflection.camelize(api, False)))

    apis = " :+: ".join(apis)

    app_name =  inflection.camelize(config['app']['name'])
    app_name_lower = inflection.camelize(config['app']['name'], False)

    service = read_template("api-service")
    out = service.replace("@@package@@", config['package']).replace("@@app@@", app_name).replace("@@appLower@@", app_name_lower).replace("@@api_list@@", apis)
    file_name = os.path.join(config['base_path'], "%sApi.scala" % (app_name))

    fd = open(file_name, "w+")
    fd.write(out)
    fd.close()
    SCALA_FILES.append(file_name)

def build_env(app_config):
    template = read_template("env")
    app = app_config["app"]
    file_name = os.path.join(app['source_directory'], ".env")
    port = str(app.get("port", "8888"))
    env = app.get("environment", "development")
    rollbar_access_key = app.get("rollbar_access_key", "1234")

    out = template.replace("@@port@@", port).replace("@@env@@", env).replace("@@rollbar_access_key@@", rollbar_access_key)

    fd = open(file_name, "w+")
    fd.write(out)
    fd.close()

if __name__ == "__main__":
    #read in our configs
    model_stream = open("models.yml")
    app_stream = open("project.yml")

    app_data = load(app_stream, Loader=Loader)

    gen_options = app_data['generator']

    app_data['package'] = ("%s.%s" % (app_data['app']['namespace'], app_data['app']['name'])).lower()
    app_data['base_path'] = os.path.join(app_data['app']['source_directory'], "src/main/scala/%s" % (app_data['app']['name'].lower()))
    app_data['config_path'] = os.path.join(app_data['app']['source_directory'], "config")

    if not os.path.exists(app_data['config_path']):
        os.makedirs(app_data['config_path'])

    model_data = load(model_stream, Loader=Loader)


    #make sure that all models have an id and timestamps
    for index in range(len(model_data)):
        model = model_data[index]
        cols = model["columns"]
        if "id" not in cols:
            model_data[index]["columns"].insert(0, {
                "name": "id",
                "auto_increment" : True,
                "type" : "bigint",
                "primary_key" : True
            })

        if 'created_at' not in cols:
            model_data[index]["columns"].append({
                "name": "created_at",
                "type" : "timestamp",
                "default": "NOW()"
            })

        if 'updated_at' not in cols:
            model_data[index]["columns"].append({
                "name": "updated_at",
                "type" : "timestamp",
                "default": "NOW()"
            })

    if key_with_default(gen_options, "sbt", True):
        print("Generating project build files...")
        sbt_builder = SbtBuilder(app_data)

    if key_with_default(gen_options, "models", True):
        print("Generating models...")
        model_builder = ModelBuilder(model_data, app_data)
        access_builder =  DbAccessBuilder(model_data, app_data)

    if key_with_default(gen_options, "migrations", True):
        print("Generating migrations...")
        migration_builder = MigrationBuilder(model_data, app_data)

    if key_with_default(gen_options, "encoders", True):
        print("Generating JSON encoders...")
        encoder_builder = EncoderBuilder(model_data, app_data)

    if key_with_default(gen_options, "api", True):
        print("Generating API...")
        api_builder = ApiBuilder(model_data, app_data)
        build_boot(app_data)
        build_api_service(model_data, app_data)

    build_env(app_data)

    if key_with_default(gen_options, "format_source", True):
      print("Formatting Scala sources...")
      format_scala()

    message = """Your project is now available in %s. To run, simply
    cd into %s and then type "sbt run". Your API will be available at http://localhost:%s/api/[model_name]""" % (app_data["base_path"], app_data["base_path"], app_data["app"]["port"])

    print
    print(message)
