package appnamespace.app.migrations

import com.imageworks.migration._

class Migrate_20160623140657442_CreateY {

  val table = "y"

  def up() = {
    createTable(table) { t =>
      t.bigint("i", NotNull)
      t.varchar("n", NotNull)
      t.varchar("a", NotNull)
    }

  }

  def down() {
    dropTable(table)
  }
}
