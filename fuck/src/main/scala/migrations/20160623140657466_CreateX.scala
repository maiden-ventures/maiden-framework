package appnamespace.app.migrations

import com.imageworks.migration._

class Migrate_20160623140657466_CreateX {

  val table = "x"

  def up() = {
    createTable(table) { t =>
      t.bigint("fred", NotNull, PrimaryKey, Default("100"))
      t.varchar("stupid_name", NotNull)
      t.varchar("abc", Limit(100))
      t.datetime("zzz", NotNull)
    }

    addIndex(table, Array("abc"), Name("x_abc_index"))

    addForeignKey(on("fucker" -> "name"),
                  references("x" -> "name"),
                  OnDelete(Cascade),
                  Name("fk_fucker_name"))

  }

  def down() {
    dropTable(table)
  }
}
