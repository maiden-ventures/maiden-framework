import os
from datetime import datetime
from helpers import *
import time
from app import Model


TYPE_MAPPINGS = {
  "varchar": "VarcharType",
  "integer": "IntegerType",
  "bigint": "BigintType",
  "boolean": "BooleanType",
  "timestamp": "TimestampType",
  "decimal": "DecimalType"
}

class MigrationBuilder:

  def __init__(self, app):
    self.app = app

    self.create_table_template = """
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

    self.other_migration_template = """
    package %s.migrations

    import com.imageworks.migration._

    class %s extends Migration {
      val table = "%s"

      def up() = {
        %s

        %s

        %s
      }

      def down() = {

      }
    }
    """

    self.build()

  def drop_column(self, table, col):
    self.columns.append("""removeColumn(table, "%s" )""" %(col.db_name))

  def add_column(self, table, col):
      modifiers = ["NotNull"]

      modifiers = ', '.join(col.migration_modifiers)

      if col.db_type.startswith("decimal"):
        col.db_type = "decimal"

      if len(col.migration_modifiers) > 0:
        self.columns.append("""addColumn(table, "%s", %s, %s)""" % (col.db_name, TYPE_MAPPINGS[col.db_type], col.migration_modifiers))
      else:
        self.columns.append("""addColumn(table, "%s", %s)""" % (col.db_name, TYPE_MAPPINGS[col.db_type]))

        self.create_index(table, col)


  def alter_column(self, table, col):
      modifiers = ["NotNull"]

      modifiers = ', '.join(col.migration_modifiers)

      if col.db_type.startswith("decimal"):
        col.db_type = "decimal"

      if len(col.migration_modifiers) > 0:
        if col.migration_modifiers != "":
          self.columns.append("""alterColumn(table, "%s", %s, %s)""" % (col.db_name, TYPE_MAPPINGS[col.db_type], col.migration_modifiers))
        else:
          self.columns.append("""alterColumn(table, "%s", %s)""" % (col.db_name, TYPE_MAPPINGS[col.db_type]))
      else:
        if col.migration_modifiers != "":
          self.columns.append("""alterColumn(table, "%s", %s, %s)""" % (col.db_name, TYPE_MAPPINGS[col.db_type]), col.migration_modifiers)
        else:
          self.columns.append("""alterColumn(table, "%s", %s)""" % (col.db_name, TYPE_MAPPINGS[col.db_type]))

      self.create_index(table, col)

  def create_index(self, table, col):
      if col.index:
          index_name = "%s_%s_index"  % (table, col.db_name)
          self.indexes.append("""addIndex(table, Array("%s"), Name("%s"))""" % (col.db_name, index_name))

      if col.unique_index:
          index_name = "%s_%s_index"  % (table, col.db_name)
          self.indexes.append("""addIndex(table, Array("%s"), Unique, Name("%s"))""" % (col.db_name, index_name))

      if col.references:
          ref = col.references
          s = """addForeignKey(on("%s" -> "%s"),
          references("%s" -> "%s"),
          OnDelete(%s),
          Name("%s"))""" % (ref.table, ref.column, ref.ref_table, ref.ref_column, ref.on_delete, ref.name)

          self.references.append(s)


  def build_column(self, table, col):
      modifiers = ["NotNull"]

      modifiers = ', '.join(col.migration_modifiers)

      if col.db_type.startswith("decimal"):
        col.db_type = "decimal"

      if len(col.migration_modifiers) > 0:
        self.columns.append("""t.%s("%s", %s)""" % (col.db_type, col.db_name, col.migration_modifiers))
      else:
        self.columns.append("""t.%s("%s")""" % (col.db_type, col.db_name))

      if col.index:
          index_name = "%s_%s_index"  % (table, col.db_name)
          self.indexes.append("""addIndex(table, Array("%s"), Name("%s"))""" % (col.db_name, index_name))

      if col.unique_index:
          index_name = "%s_%s_fk"  % (table, col.db_name)
          self.indexes.append("""addIndex(table, Array("%s"), Unique, Name("%s"))""" % (col.db_name, index_name))

      if col.references:
          ref = col.references
          s = """addForeignKey(on("%s" -> "%s"),
          references("%s" -> "%s"),
          OnDelete(%s),
          Name("%s"))""" % (ref.table, ref.column, ref.ref_table, ref.ref_column, ref.on_delete, ref.name)

          self.references.append(s)


  def build(self):

    self.migration_dir = os.path.abspath(os.path.join(self.app.base_path,  "components/migrations"))

    for model_name, meta in self.app.migrations.items():

        template = self.other_migration_template


        migration_prefix = ""


        for key, payload in meta.items():
          self.columns = []
          self.indexes = []
          self.references = []
          if payload == []: continue

          if key == "create_table" and payload != {}:
            template = self.create_table_template
            migration_name = "Create%s" % (camelize(model_name))

            model = Model(payload, self.app.casing)
            for col in model.columns:
                self.build_column(model.db_name, col)
            self.write_migration(template, migration_name,  model)

          elif key == "added":

            for p in payload:
              migration_name = "Add%sTo%s" % (camelize(p['name']), camelize(model_name))
              m = {"name": model_name, "columns": [p,]}
              model = Model(m, self.app.casing)
              for col in model.columns:
                self.add_column(model.db_name, col)
              self.write_migration(template, migration_name,  model)
              self.columns = []
              self.indexes = []
              self.references = []

          elif key == "removed":

            for p in payload:
              print p
              migration_name = "Remove%sFrom%s" % (camelize(p['name']), camelize(model_name))
              m = {"name": model_name, "columns": p}
              model = Model(m, self.app.casing)
              for col in model.columns:
                  self.drop_column(model.db_name, col)
              self.write_migration(template, migration_name,  model)
              self.columns = []
              self.indexes = []
              self.references = []

          elif key == "altered":
            for p in payload:
              migration_name = "Alter%sFrom%s" % (camelize(p['name']), camelize(model_name))
              m = {"name": model_name, "columns": payload}
              model = Model(m, self.app.casing)
              for col in model.columns:
                  self.alter_column(model.db_name, col)
              self.write_migration(template, migration_name,  model)
              self.columns = []
              self.indexes = []
              self.references = []


  def write_migration(self, template, migration_name, model):
        now_ts = str(datetime.now()).split(".")[0].replace("-","").replace(":","").replace(".","").replace(" ", "")
        class_name = "Migrate_%s_%s"  % (now_ts, migration_name)

        file_name = "%s.scala" % (class_name.replace("Migrate_", ""))

        package = self.app.package
        column_str = "\n".join(self.columns)
        indexes_str = "\n".join(self.indexes)
        references_str = "\n".join(self.references)
        table_str = model.db_name

        out = template % (package, class_name, table_str, column_str, indexes_str, references_str)
        write(os.path.join(self.migration_dir, file_name), out)
        time.sleep(1)
