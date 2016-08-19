from helpers import *
from misc_builders import *

class App:

    def __init__(self, info, model_info, casing):

        self.info = info
        self.app_info = self.info["app"]
        self.db_info = self.info["db"]
        self.social_info = self.info["social"]

        self.casing = casing

        self.model_info = model_info
        #build out our models
        self.models = [Model(model, casing) for model in model_info]

        self.migrations = self.info['migrations']

        #build out meta info
        self.name = camelize(self.app_info["name"])
        self.name_lower = underscore(self.name).lower()

        self.namespace = self.app_info["namespace"].lower()
        self.package = "%s.%s" % (self.namespace, self.name_lower)
        self.payload_key = self.app_info.get("payload_key", "payload")
        self.port = self.app_info["port"]
        self.http_port = self.app_info.get("https_port", "")
        self.certificate_path = self.app_info.get("certificate_path", "")
        self.key_path = self.app_info.get("key_path", "")

        self.source_directory = self.app_info["source_directory"]
        self.base_path = os.path.join(self.source_directory, "src/main/scala/%s" % (self.name_lower))
        self.config_path = os.path.join(self.source_directory, "config")
        self.environment = self.app_info.get("envirornment", "development")
        self.max_request_size = self.app_info.get("max_request_size", "10")
        self.https_port = self.app_info.get("https_port", "")
        self.certificate_path = self.app_info.get("certificate_path", "")
        self.key_path = self.app_info.get("key_path", "")

        if 'generator' in info and "prompt_overwrite" in info['generator']:
            self.prompt_overwrite = info['generator']['prompt_overwrite']
        else:
            self.prompt_overwrite = True


        #make sure our paths exist
        print("Creating directories under %s" % (self.source_directory))
        make_dir(self.config_path)
        make_dir(os.path.join(self.source_directory, "project"))
        make_dir(self.base_path)
        #build out our data structures
        self.security = security_info(self.app_info)
        #self.database  = build_database(self.app_info)
        self.social = self.build_social()

    def build_security(self):
        pass

    def build_database(self):
        pass

    def build_social(self):
        pass

class Model:
    def __init__(self, model_info, casing):
        self.info = model_info


        self.casing = casing
        #whether to generate an API for this model
        self.generate_api = self.info.get("generate_api", True)
        #whether to generate model classes for this model
        self.generate_model = self.info.get("generate_model", True)

        self.generate_migration = self.info.get("generate_migration", True)

        try:
            self.name = camelize(self.info["name"])
        except:
            print self.info
            raise("DONE")
        self.name_lower = camelize(self.info["name"], False)
        if "db_name" in model_info:
            self.db_name = model_info["db_name"]
        elif casing == "snake_case":
            self.db_name = underscore(self.name).lower()
        elif casing == "camel_case":
            self.db_name = camelize(self.name, False)
        else:
            self.db_name = self.name
        self.query_name = "%sQuery" % (self.name_lower)
        self.build_columns(casing)

    def build_columns(self, casing):
        self.columns = [Column(self, column, casing) for column in self.info["columns"]]

class ForeignKey:

    def __init__(self, col):
        self.ref_table = col.info["references"]["table"]
        self.ref_column = col.info["references"]["column"]
        self.ref_type = col.info["references"].get("type", "ONE_TO_MANY")
        self.column = col.db_name
        self.table = col.table
        self.field_name = col.name

        if 'name' in col.info['references']:
            self.scala_name =col.info['references']['name']
        else:
            self.scala_name = self.table

        self.scala_name = inflection.camelize(self.scala_name, False)

        self.on_delete = camelize(col.info["references"].get("on_delete", "SetDefault"))
        self.name = "fk_%s_%s_%s_%s" % (self.table, self.field_name, self.ref_table, self.ref_column)

    def __str__(self):
        return "ref_table: %s, ref_column: %s, table: %s, column: %s" % (self.ref_table, self.ref_column, self.table, self.field_name)

class Column:
    def __init__(self, model, col_info, casing):

        self.info = col_info
        self.table = model.db_name
        self.model = model.name

        self.name = camelize(self.info["name"], False)
        self.named_type = self.info["type"]
        db_map = DB_MAP[self.info["type"]]
        self.db_type = DB_MAP[self.named_type]["db"]
        self.index = self.info.get("index", False)
        self.limit = self.info.get("limit", None)
        self.precision = self.info.get("precision", "10,2")

        if "db_name" in self.info:
            self.db_name = self.info["db_name"]
        else:
            if casing == "snake_case":
                self.db_name = underscore(col_info["name"]).lower()
            elif casing == "camel_case":
                self.db_name = camelize(self.name, False)
            else:
                self.db_name = col_info["name"]

        #self.db_name = self.info.get("db_name", col_info["name"])
        self.scala_type = db_map["scala"]
        self.validations = col_info.get("validations", [])
        self.formatters = col_info.get("formatters", [])

        self.index = col_info.get("index", False)
        self.unique_index = col_info.get("unique_index", False)
        self.nullable = col_info.get("nullable", False)

        if 'validations' in db_map: self.validations.extend(db_map["validations"])
        #if 'formatters' in db_map: self.formatters.extend(db_map["formatters"])

        self.build_references()
        self.build_migration_modifiers()


    def build_references(self):
        self.references = None
        if 'references' in self.info:
            self.references = ForeignKey(self)

    def build_migration_modifiers(self):
        modifiers = ["NotNull"]
        if exists_and_true(self.info, 'auto_increment'): modifiers.append("AutoIncrement")
        if exists_and_true(self.info, 'primary_key'): modifiers.append("PrimaryKey")
        if exists_and_true(self.info, 'nullable'): modifiers.remove("NotNull")
        if 'default' in self.info: modifiers.append("""Default("%s")""" % (self.info['default']))
        if 'limit' in self.info: modifiers.append("""Limit(%s)""" % (self.info['limit']))

        if 'limit' not in self.info and self.scala_type in ('String'):
            modifiers.append("Limit(255)")

        #handle precision for decimals
        if self.scala_type in ('BigDecimal',):
            if 'precision' not in self.info:
                modifiers.append("Precision(10)")
                modifiers.append("Scale(2)")
            else:
                p = self.info['precision'].split(",")
                precision = p[0]
                scale = p[1]
                modifiers.append("Precision(%s)" % (precision))
                modifiers.append("Scale(%s)" % (scale))

        self.migration_modifiers = ', '.join(modifiers)
