package appnamespace.app.migrations

import com.imageworks.migration._

class Migrate_20160623140657059_CreateFuck {

  val table = "fuck"

  def up() = {
    createTable(table) { t =>
      t.bigint("id", NotNull)
    }

  }

  def down() {
    dropTable(table)
  }
}
