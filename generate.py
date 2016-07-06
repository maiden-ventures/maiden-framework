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


  def build_column(self, table, info):
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

          on_delete = "SetDefault"
          if 'on_delete' in ref:
              on_delete = ref['on_delete']

          s = """addForeignKey(on("%s" -> "%s"),
          references("%s" -> "%s"),
          OnDelete(%s),
          Name("%s"))""" % (inflection.underscore("table").lower(), info["db_name"], ref_table, ref_column, on_delete, ref_name)

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
            self.build_column(model['db_name'], col)


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
            datasource_driver = "org.postgresql.ds.PGSimpleDataSource"
            database_driver = "org.postgresql.Driver"
            database_jdbc_name = "postgresql"

        elif db_type == "mysql":
            default_port = "3336"
            dialect_name = "MySQL"

        else:
            throw(Exception("UNKNOWN DATABASE TYPE"))
        db_port = db.get("port", default_port)


        template = read_template("config")



        sec = security_info(self.config)

        props = template.replace("@@host@@", db_host)\
                        .replace("@@dbPort@@", db_port)\
                        .replace("@@appPort@@", str(self.config["app"]["port"]))\
                        .replace("@@db@@", db_name)\
                        .replace("@@databaseJdbcName@@", database_jdbc_name)\
                        .replace("@@user@@", db_user)\
                        .replace("@@password@@", db_password)\
                        .replace("@@package@@", self.config["package"])\
                        .replace("@@sourceDir@@", self.config["base_path"])\
                        .replace("@@databaseDriver@@", database_driver)\
                        .replace("@@datasourceClassName@@", datasource_driver)\
                        .replace("@@environment@@", self.config["app"]["environment"])\
                        .replace("@@appId@@", self.config["app"]["name"]) \
                        .replace("@@securityConfig@@", sec["security_config"])

        #now write out the application.properties
        fd = open(os.path.join(self.config['config_path'], "maiden-%s.conf" % (self.config["app"]["environment"])), "w+")
        fd.write(props)

        fd.close()

        schema = read_template("schema")
        schema_list = []
        for m in self.models:
            lname = inflection.camelize(m["name"], False)
            uname = inflection.camelize(m["name"], True)

            if lname == "user" and not 'db_name' in m:
                m['db_name'] = "users"
            elif 'db_name' not in m:
                m['db_name'] = lname

            s = 'lazy val %sQuery = quote(query[%s].schema(_.entity("%s")))' % (lname, uname, inflection.underscore(m['db_name']))
            schema_list.append(s)

        schema_list_str = "\n".join(schema_list)

        out = schema.replace("@@package@@", self.config["package"]).replace("@@schemaList@@", schema_list_str).replace("@@appName@@", inflection.camelize(self.config["app"]["name"]))

        fd = open(os.path.join(self.config['base_path'], "models/Schema.scala"), "w+")
        fd.write(out)
        fd.close()

class ModelBuilder:

  def __init__(self, data, config):
    self.data = data
    self.config = config
    self.build()

  def build_timestamp_range(self, model_name, field_name):
      s = """def findBy%sRange(start: LocalDateTime, end: LocalDateTime) = {
      val q = quote {
        (s: LocalDateTime, e: LocalDateTime) =>
          %s.filter(c => c.%s > s && c.%s < e)
      }
      db.run(q)(start, end)
    }""" % (inflection.camelize(field_name), self.query_name, inflection.camelize(field_name, False), inflection.camelize(field_name, False))

      return s

  def __build_update_matcher(self, field_name):
      s = """%s match {
        case Some(v) => if (v != existing.%s) existing.%s = v
        case _ => ()
      }""" % (field_name, field_name, field_name)

      return s

  def build_references(self, model_name, field_name, model2_name, field2_name):
      #grab a foreign key reference
      table1 = inflection.camelize(model_name)
      table2 = inflection.camelize(model2_name)
      field1 = inflection.camelize(field_name, False)
      field2 = inflection.camelize(field2_name, False)

      s = """def get%s(id: Long) = {
      val q = quote {
        query[%s].join(query[%s]).on((q1,q2) => q1.%s == q2.%s && q2.id == lift(id))
      }
      db.run(q).map(x => x._1)
    }""" % (table1, table1, table2, field1, field2)
      return s

  def build(self):

    db_type = self.config['db']['driver']

    db_driver_name = ""
    if db_type == "postgres": db_driver_name = "PostgresDB"
    if db_type == "mysql": db_driver_name = "MySqlDB"

    models_dir = os.path.abspath(os.path.join(self.config["base_path"], "models"))

    #make sure the models directory exists
    if not os.path.exists(models_dir):
        os.makedirs(models_dir)

    for model in self.data:

        columns = []

        template = read_template("model")

        self.model_name = inflection.camelize(model["name"])
        self.query_name = "%sQuery" % (inflection.camelize(model["name"], False))
        model_path= os.path.join(models_dir, "%s.scala" % (self.model_name))
        self.app_name = inflection.camelize(self.config["app"]["name"])

        fk_accessors = []

        raw_columns = [(inflection.camelize(c["name"], False), DB_TO_SCALA[c["type"]]) for c in model["columns"]]
        create_columns = filter(lambda x: x[0] not in ("createdAt", "updatedAt", "id"), raw_columns)

        if 'db_name' not in model:
            model['db_name'] = model['name']

        #get all possible references to this model
        ref_fields = []
        reference_methods = ""
        for m in self.data:
            if m['name'] != model["name"]:
                for c in m['columns']:
                    if 'references' in c:
                        if c['references']['table'] == model['name']:
                            _ref = c['references']
                            reference_methods += (self.build_references(inflection.camelize(m['name']), inflection.camelize(c['name'], False),  inflection.camelize(model["name"], True), inflection.camelize(_ref["column"], False)))

                            #(name, type, local_model, local_field, ref_model, ref_field)
                            ref_fields.append((inflection.camelize(m["name"], False),
                                               DB_TO_SCALA[c["type"]],
                                               inflection.camelize(_ref["table"]), inflection.camelize(_ref["column"], False),
                                               inflection.camelize(m["name"]), inflection.camelize(c["name"], False)))

        for col in model["columns"]:
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

          like_columns = [c for c in raw_columns if c[1] == "String"]
          ref_columns = [c for c in model['columns'] if 'references' in c]


        base_fields = ",".join(columns)

        template = template.replace("@@baseFields@@", base_fields)
        magic_methods = []

        findByTemplate = read_template("models/findby")
        rangeByTemplate = read_template("models/rangeby")
        deleteByTemplate = read_template("models/deleteby")
        likeTemplate = read_template("models/findbylike")
        updateTemplate = read_template("models/update")
        createTemplate = read_template("models/create")

        magic_methods_str = ""

        for c in raw_columns:
            colUpper = inflection.camelize(c[0])
            colLower = inflection.camelize(c[0], False)
            colType = c[1]
            if c[0] == "String":
                full_template = "%s\n\n%s\n\n%s\n\n%s" % (findByTemplate, rangeByTemplate, likeTemplate, deleteByTemplate)
            else:
                full_template = "%s\n\n%s\n\n%s" % (findByTemplate, rangeByTemplate, deleteByTemplate)


            magic_methods_str += full_template.replace("@@columnUpper@@", colUpper).replace("@@columnLower@@", colLower).replace("@@columnType@@", colType).replace("@@queryName@@", self.query_name)

        for r in ref_columns:
            reference_methods += "\n\n" + self.build_references(r["references"]["table"], r["references"]["column"], self.model_name, r["name"])

        #now do create and update
        update_params = ", ".join(["%s: Option[%s] = None" % (c[0], c[1]) for c in create_columns])
        create_params = ", ".join(["%s: %s" % (c[0], c[1]) for c in create_columns])
        model_create_params = ", ".join(["%s = %s" % (c[0],c[0]) for c in create_columns])
        ref_fields_str = ",\n".join(["%s: List[%s] = List.empty" % (r[0], r[4]) for r in ref_fields])
        ref_constructor_fields = ", ".join(["%s = %s" % (r[0], r[0]) for r in ref_fields])
        ref_from_db = ", ".join(["%s = %s.get%s(t.id)" % (r[0], self.model_name, r[4]) for r in ref_fields])

        ref_yields = ", ".join([x[0] for x in ref_fields])
        ref_count = len(ref_fields)

        #test2  <- query[Test2].filter(x => x.firstName == lift(test.firstName))

        if ref_count > 0:
          ref_comprehensions = []
          #(name, type local_model, local_field, ref_model, ref_field)
          for r in ref_fields:
              ref_comprehensions.append("%s <-query[%s].filter(x => x.%s == lift(%s.%s))" % (r[0], r[4], r[5], inflection.camelize(r[2], False), r[3]))

          getallrefs = read_template("models/getallrefs").replace("@@refComprehensions@@", "\n".join(ref_comprehensions)) \
                      .replace("@@refYields@@", ref_yields) \
                      .replace("@@refCount@@", str(ref_count)) \
                      .replace("@@lowerCaseModel@@", inflection.camelize(self.model_name, False))

          magic_methods_str += "\n%s\n" % (getallrefs)
          full_response_build = """def build(t: @@model@@): @@model@@FullResponse = {
            val refs = @@model@@.getAllRefs(t)
            (@@model@@FullResponse.apply _) tupled (@@model@@.unapply(t).get ++  refs.get)
          }"""
        else:
          magic_methods_str += "\ndef getAllRefs(%s: %s) = None" % (inflection.camelize(m["name"], False), inflection.camelize(m["name"]))
          full_response_build = """def build(t: @@model@@): @@model@@FullResponse = {
            (@@model@@FullResponse.apply _) tupled (@@model@@.unapply(t).get)
          }"""

        full_response_build = full_response_build.replace("@@model@@", inflection.camelize(self.model_name))

        matches = []
        for c in create_columns:
          matches.append(self.__build_update_matcher(inflection.camelize(c[0], False)))

        update_matches = "\n\n".join(matches)

        magic_methods_str += "\n\n" + createTemplate.replace("@@createParams@@", create_params) \
                             .replace("@@modelCreateParams@@", model_create_params)

        magic_methods_str += "\n\n" + updateTemplate.replace("@@updateParams@@", update_params) \
                             .replace("@@updateMatches@@", update_matches)


        template = template.replace("@@magicMethods@@", magic_methods_str) \
          .replace("@@referenceMethods@@", reference_methods)



        if len(ref_fields_str) > 0: ref_fields_str = ", " +  ref_fields_str
        if len(ref_constructor_fields) > 0: ref_constructor_fields = ", " +  ref_constructor_fields
        if len(ref_from_db) > 0: ref_from_db = ", " +  ref_from_db


        base_constructor_fields = ", ".join(["%s = t.%s" % (inflection.camelize(r["name"], False), inflection.camelize(r["name"], False)) for r in model["columns"]])

        #TODO: Missing setReferencedColumn, removeReferencedColumn

        template = template.replace("@@model@@", self.model_name) \
                   .replace("@@appNameUpper@@", self.app_name) \
                   .replace("@@refFields@@", ref_fields_str) \
                   .replace("@@dbDriverName@@", db_driver_name)\
                   .replace("@@refConstructorFields@@", ref_constructor_fields) \
                   .replace("@@refFromDBFields@@", ref_from_db) \
                   .replace("@@baseConstructorFields@@", base_constructor_fields) \
                   .replace("@@package@@", self.config["package"]) \
                   .replace("@@queryName@@", self.query_name) \
                   .replace("@@fullResponseBuild@@", full_response_build) \
                   .replace("@@baseModelFieldCount@@", str(len(model["columns"])))

        fd = open(model_path, "w+")
        fd.write(template)
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

        self.security_import = security_info(config)['security_import']
        self.build()

    def build(self):
        for model in self.models:
            param_str = """param("%s").as[%s]"""

            all_model_cols = ["%s: %s" % (inflection.camelize(c["name"], False), DB_TO_SCALA[c["type"]]) for c in model['columns']]
            all_model_cols_str = ", ".join(all_model_cols)

            cols = [(inflection.camelize(c["name"], False), DB_TO_SCALA[c["type"]]) for c in model['columns'] if c["name"] not in ("id", "created_at", "updated_at")]
            create_args = " :: ".join([param_str % (c[0], c[1]) for c in cols])
            create_param_args = " :: ".join(['param("%s").as[%s]' % (c[0], c[1]) for c in cols])
            create_params = ", ".join(["%s: %s" % (c[0], c[1]) for c in cols])
            model_creation_args = ", ".join([c[0] for c in cols])

            optional_params = " :: ".join(['paramOption("%s").as[%s]' % (c[0], c[1]) for c in cols])
            param_list = ", ".join(["%s: Option[%s]" % (c[0], c[1]) for c in cols])
            update_params = ", ".join([c[0] for c in cols])

            out = self.template.replace("@@model@@", inflection.camelize(model['name'], True))\
                               .replace("@@lowerCaseModel@@", inflection.camelize(model['name'],False)) \
                               .replace("@@package@@", self.config['package'])\
                               .replace("@@securityImport@@", self.security_import)\
                               .replace("@@createArgs@@", create_args)\
                               .replace("@@createParamArgs@@", create_param_args)\
                               .replace("@@createParams@@", create_params)\
                               .replace("@@modelCreationArgs@@", model_creation_args)\
                               .replace("@@modelColumns@@", all_model_cols_str)\
                               .replace("@@optionalParams@@", optional_params)\
                               .replace("@@paramList@@", param_list).replace("@@updateParams@@", update_params)

            fd = open(os.path.join(self.api_dir, "%s.scala" % (inflection.camelize(model['name']))), "w+")
            fd.write(out)
            fd.close()
        SCALA_FILES.append(self.api_dir)

def build_boot(app_data):
    template = read_template("boot")
    app_name = app_data['app']['name']
    package = app_data['package']

    out = template.replace("@@package@@", package)\
                  .replace("@@appNameUpper@@", inflection.camelize(app_name))

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
        apis.append("%sApi.%sApi" % (inflection.camelize(api), inflection.camelize(api, False)))

    apis = " :+: ".join(apis)

    app_name =  inflection.camelize(config['app']['name'])
    app_name_lower = inflection.camelize(config['app']['name'], False)

    service = read_template("api-service")
    out = service.replace("@@package@@", config['package'])\
                 .replace("@@app@@", app_name)\
                 .replace("@@appLower@@", app_name_lower)\
                 .replace("@@api_list@@", apis)
    file_name = os.path.join(config['base_path'], "%sApi.scala" % (app_name))

    fd = open(file_name, "w+")
    fd.write(out)
    fd.close()
    SCALA_FILES.append(file_name)

def build_logback(app_config):
    template = read_template("logback")
    appName = app_config["app"]["name"].lower()
    file_name = os.path.join(app_config["app"]['source_directory'], "config/logback.xml")

    out = template.replace("@@appName@@", appName)

    fd = open(file_name, "w+")
    fd.write(out)
    fd.close()

def security_info(app_config):
    sec = {}

    #read in security (if it exsists)
    if 'security' in app_config['app']:
        base_sec = app_config['app']['security']
        #need to somehow generate the correct config here...
        if base_sec['method'] == "token":
            sec['security_import'] ="import maiden.auth.token.TokenAuth._"
            access_token = base_sec['access_token']
            if 'param_name' in base_sec:
                param_name = base_sec['param_name']
            else:
                sec_param_name = "access_token"

            sec['security_config'] = """
app.security.param_name="%s"
app.security.access_token="%s"
            """ % (param_name,  access_token)
        #add more here
    else:
        sec['security_import'] = "import maiden.auth.anon.AnonAuth._"
        sec['security_config'] = ""

    return sec


#some default models to add if the user selects
#per-user authentication
DEFAULT_USER_MODEL = {
    "name": "Users",
    "columns": [
        {"name": "user_name", "type": "varchar", "limit": 64,"index": True},
        {"name": "access_token", "type": "varchar", "limit": 128, "index": True}
    ]
}

DEFAULT_SOCIAL_USER_MODEL = {
    "name": "SocialUser",
    "columns": [
        {"name": "user_id", "type": "bigint","index": True},
        {"name": "provider", "type": "varchar", "limit": 100, "index": True},
        {"name": "uid", "type": "varchar", "limit": 255, "index": True},
        {"name": "access_token", "type": "varchar", "limit": 1024, "index": True},
        {"name": "secret_key", "type": "varchar", "limit": 1024, "nullable": True, "index": True},
        {"name": "extra", "type": "varchar", "limit": 8192, "nullable": True}
    ]
}


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

    #automatically add user tables if selected
    if 'users' in app_data['app']:
        model_data.append(DEFAULT_USER_MODEL)
        model_data.append(DEFAULT_SOCIAL_USER_MODEL)

    #make sure that all models have an id and timestamps
    for index in range(len(model_data)):
        model = model_data[index]
        cols = model["columns"]

        if 'db_name' not in model:
            model_data[index]["db_name"] = inflection.underscore(model["name"])

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

    build_logback(app_data)

    if key_with_default(gen_options, "format_source", True):
      print("Formatting Scala sources...")
      format_scala()

    project_dir = os.path.realpath(app_data["app"]["source_directory"])
    message = """Your project is now available in %s. To run, simply
    cd into %s and then type "sbt run". Your API will be available at http://localhost:%s/api/[model_name]""" % (project_dir, project_dir, app_data["app"]["port"])

    print
    print(message)
