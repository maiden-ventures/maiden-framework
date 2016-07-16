import os
from datetime import datetime
from helpers import *
import time

class MigrationBuilder:

  def __init__(self, app):
    self.app = app
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


  def build_column(self, table, col):
      modifiers = ["NotNull"]

      modifiers = ', '.join(col.migration_modifiers)

      if len(col.migration_modifiers) > 0:
        self.columns.append("""t.%s("%s", %s)""" % (col.db_type, col.db_name, col.migration_modifiers))
      else:
        self.columns.append("""t.%s("%s")""" % (col.db_type, col.db_name))


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


  def build(self):

    migration_dir = os.path.abspath(os.path.join(self.app.base_path,  "migrations"))

    for model in self.app.models:
        self.columns = []
        self.indexes = []
        self.references = []

        now_ts = str(datetime.now()).split(".")[0].replace("-","").replace(":","").replace(".","").replace(" ", "")

        migration_name = "Create%s" % (model.name)
        class_name = "Migrate_%s_%s"  % (now_ts, migration_name)
        #table_name = underscore(model["name"]).lower()
        file_name = "%s.scala" % (class_name.replace("Migrate_", ""))

        for col in model.columns:
            self.build_column(model.db_name, col)


        package = self.app.package
        column_str = "\n".join(self.columns)
        indexes_str = "\n".join(self.indexes)
        references_str = "\n".join(self.references)
        table_str = model.db_name

        out = self.template % (package, class_name, table_str, column_str, indexes_str, references_str)
        write(os.path.join(migration_dir, file_name), out)
        time.sleep(2)
