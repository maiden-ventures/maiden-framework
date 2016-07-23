from helpers import *
from misc_builders import security_info



class DbAccessBuilder:
    def __init__(self, app):
        self.app = app
        self.build()

    def build(self):
        db = self.app.db_info
        database_type = db['driver']
        db_name = db['name']
        db_user = db['user']
        db_password = db['password']
        db_host = db.get("host", "localhost")

        default_port = ""
        dialect_name = ""

        #TODO: Add support for cassandra
        template = "postgres-props"
        if database_type == "postgres":
            default_port = "5432"
            datasource_driver = "org.postgresql.ds.PGSimpleDataSource"
            database_driver = "org.postgresql.Driver"
            database_jdbc_name = "postgresql"
            db_driver_name = "PostgresDB"

        elif database_type == "mysql":
            default_port = "3336"
            db_driver_name = "MySqlDB"

        else:
            raise(Exception("UNKNOWN DATABASE TYPE"))
        db_port = db.get("port", default_port)

        template = read_template("config")
        sec = security_info(self.app.app_info)

        props = template.replace("@@host@@", db_host)\
                        .replace("@@dbPort@@", db_port)\
                        .replace("@@appPort@@", str(self.app.port))\
                        .replace("@@db@@", db_name)\
                        .replace("@@databaseJdbcName@@", database_jdbc_name)\
                        .replace("@@user@@", db_user)\
                        .replace("@@password@@", db_password)\
                        .replace("@@package@@", self.app.package)\
                        .replace("@@sourceDir@@", self.app.base_path)\
                        .replace("@@databaseDriver@@", database_driver)\
                        .replace("@@datasourceClassName@@", datasource_driver)\
                        .replace("@@environment@@", self.app.environment)\
                        .replace("@@appId@@", self.app.name) \
                        .replace("@@securityConfig@@", sec["security_config"]) \
                        .replace("@@httpInterface@@", str(self.app.port))\
                        .replace("@@httpsInterface@@", str(self.app.https_port))\
                        .replace("@@keyPath@@", self.app.key_path)\
                        .replace("@@certificatePath@@", self.app.certificate_path) \
                        .replace("@@maxRequestSize@@", str(self.app.max_request_size))

        #now write out the application.properties
        write(os.path.join(self.app.config_path, "maiden-%s.conf" % (self.app.environment)), props)

        schema = read_template("schema")
        schema_list = []
        for m in self.app.models:
            lname = m.name_lower
            uname = m.name

            s = 'lazy val %sQuery = quote(query[%s].schema(_.entity("%s").generated(_.id)))' % (lname, uname, m.db_name)
            schema_list.append(s)

        schema_list_str = "\n".join(schema_list)

        out = schema\
              .replace("@@package@@", self.app.package)\
                       .replace("@@schemaList@@", schema_list_str)\
                       .replace("@@appName@@", self.app.name) \
                       .replace("@@dbDriverName@@", db_driver_name)

        write(os.path.join(self.app.base_path, "models/Schema.scala"), out)
