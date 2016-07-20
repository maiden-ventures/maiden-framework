from helpers import *

class ModelBuilder:

  def __init__(self, app):
    self.app = app
    self.build()

  def build_timestamp_range(self, model_name, field_name):
      s = """def findBy%sRange(start: LocalDateTime, end: LocalDateTime) = {
      val q = quote {
        (s: LocalDateTime, e: LocalDateTime) =>
          %s.filter(c => c.%s > s && c.%s < e)
      }
      db.run(q)(start, end)
    }""" % (camelize(field_name), self.query_name, camelize(field_name, False), camelize(field_name, False))

      return s

  def __build_update_matcher(self, field_name):
      s = """%s match {
        case Some(v) => if (v != existing.%s) existing.%s = Some(v)
        case _ => ()
      }""" % (field_name, field_name, field_name)

      return s

  def build_references(self, model_name, field_name, model2_name, field2_name):
      #grab a foreign key reference
      s = """def get%s(id: Long) = {
      val q = quote {
        %sQuery.join(%sQuery).on((q1,q2) => q1.%s == q2.%s && q2.id == lift(id))
      }
      db.run(q).map(x => x._1)
    }""" % (model2_name, camelize(model2_name, False), camelize(model_name, False), field2_name, field_name)
      return s

  def build(self):

    database_type = self.app.db_info['driver']

    db_driver_name = ""
    if database_type == "postgres": db_driver_name = "PostgresDB"
    if database_type == "mysql": db_driver_name = "MySqlDB"

    models_dir = os.path.abspath(os.path.join(self.app.base_path, "models"))

    #make sure the models directory exists

    for model in self.app.models:

        columns = []

        template = read_template("model")

        self.query_name = "%sQuery" % (model.name_lower)
        model_path= os.path.join(models_dir, "%s.scala" % (model.name))

        raw_columns = [(c.name, c.scala_type, c.formatters) for c in model.columns]
        create_columns = filter(lambda x: x[0] not in ("createdAt", "updatedAt", "id"), raw_columns)

        formatted_fields = []
        for c in raw_columns:
            if len(c[2]) > 0:
               formatted_fields.append("%s.%s" % (c[0], ".".join(c[2])))
            else:
                formatted_fields.append(c[0])

        formatted_columns = ",".join(formatted_fields)

        def get_scala_names(table_name, col_name):
            for m in self.app.models:
                if m.db_name == table_name:
                    for c in m.columns:
                        if c.db_name == col_name:
                            return (m.name, c.name)

        #get all possible references to this model
        ref_fields = []
        reference_methods = ""
        for m in self.app.models:
            if m.name != model.name:
                for c in m.columns:
                    if c.references:
                        if c.references.ref_table == model.db_name:
                            _ref = c.references
                            (ref_model, ref_field) = get_scala_names(_ref.table, _ref.column)
                            (tb,co) = get_scala_names(_ref.ref_table, _ref.ref_column)
                            reference_methods += "\n\n" + self.build_references(tb, co,  ref_model, ref_field)

                            #(name, type, local_model, local_field, ref_model, ref_field)
                            ref_fields.append((m.name_lower,
                                               c.scala_type,
                                               ref_model, ref_field,
                                               tb, co))

        for col in model.columns:
          col_name = col.name
          if col_name == "id":
              col_str = "  id: Option[Long] = Some(-1l)"
          elif col_name  == "createdAt":
              col_str = "  createdAt: LocalDateTime = LocalDateTime.now"
          elif col_name  == "updatedAt":
              col_str = "  updatedAt: LocalDateTime = LocalDateTime.now"
          else:
              col_str = "  %s: Option[%s]" % (col_name, col.scala_type)

          columns.append("\n%s" % (col_str))

          like_columns = [c for c in raw_columns if c[1] == "String"]
          ref_columns = [c for c in model.columns if c.references]


        base_fields = ",".join(columns)

        template = template.replace("@@baseFields@@", base_fields)
        findByTemplate = read_template("models/findby")
        rangeByTemplate = read_template("models/rangeby")
        deleteByTemplate = read_template("models/deleteby")
        likeTemplate = read_template("models/findbylike")
        updateTemplate = read_template("models/update")
        createTemplate = read_template("models/create")

        find_by_case = "\n".join(["""case "%s" => quote { %s.filter(_.%s == lift(value)) }""" % (c.name, model.query_name, c.name) for c in model.columns])

        find_by_like_case = "\n".join(["""case "%s" => quote { %s.filter(_.%s like  lift(value)) }""" % (c.name, model.query_name, c.name) for c in model.columns])

        delete_by_case = "\n".join(["""case "%s" => quote { %s.filter(_.%s == lift(value).delete) }""" % (c.name, model.query_name, c.name) for c in model.columns])

        range_by_case = "\n".join(["""case "%s" => quote { %s.sortBy(c => c.%s).drop(lift(start)).take(lift(count))) }""" % (c.name, model.query_name, c.name) for c in model.columns])



        magic_methods_str = "%s\n%s\n%s%s\n" % (
          findByTemplate.replace("@@findByCase@@", find_by_case),
          likeTemplate.replace("@@findByLikeCase@@", find_by_like_case),
          deleteByTemplate.replace("@@deleteByCase@@", delete_by_case),
          rangeByTemplate.replace("@@rangeByCase@@", range_by_case)
        )

        #now do create and update
        update_params = ", ".join(["%s: Option[%s] = None" % (c[0], c[1]) for c in create_columns])
        create_params = ", ".join(["%s: %s" % (c[0], c[1]) for c in create_columns])
        model_create_params = ", ".join(["%s = %s" % (c[0],c[0]) for c in create_columns])

        ref_fields_str = ",\n".join(["%s: List[%s] = List.empty" % (r[0], r[2]) \
                                     for r in ref_fields])
        ref_constructor_fields = ", ".join(["%s = %s" % (r[0], r[0]) for r in ref_fields])
        ref_from_db = ", ".join(["%s = %s.get%s(t.id)" % (r[0], model.name, r[4]) \
                                 for r in ref_fields])

        ref_yields = ", ".join([x[0] for x in ref_fields])
        ref_count = len(ref_fields)

        if ref_count > 0:
          ref_comprehensions = []
          #(name, type local_model, local_field, ref_model, ref_field)
          for r in ref_fields:
              ref_comprehensions.append("get%s(%s.%s.get)" %(r[2], model.name_lower, r[5]))
              #ref_comprehensions.append("%s <-query[%s].filter(x => x.%s == lift(%s.%s))" % (r[0], r[4], r[5], model.name_lower, r[3]))

          ref_comprehensions = "(" + ",".join(ref_comprehensions) + ")"
          getallrefs = read_template("models/getallrefs")\
                      .replace("@@refComprehensions@@", ref_comprehensions) \
                      .replace("@@refYields@@", ref_yields) \
                      .replace("@@refCount@@", str(ref_count)) \
                      .replace("@@lowerCaseModel@@", model.name_lower)

          magic_methods_str += "\n%s\n" % (getallrefs)
          full_response_build = """def build(t: @@model@@): @@model@@FullResponse = {
            val refs = @@model@@.getAllRefs(t)
            (@@model@@FullResponse.apply _) tupled (@@model@@.unapply(t).get ++  refs)
          }"""
        else:
          magic_methods_str += "\ndef getAllRefs(%s: %s) = None" % (m.name_lower,m.name)
          full_response_build = """def build(t: @@model@@): @@model@@FullResponse = {
            (@@model@@FullResponse.apply _) tupled (@@model@@.unapply(t).get)
          }"""

        full_response_build = full_response_build.replace("@@model@@", model.name)

        matches = []
        for c in create_columns:
          matches.append(self.__build_update_matcher(c[0]))

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


        base_constructor_fields = ", ".join(["%s = t.%s" % (r.name, r.name) for r in model.columns])

        #TODO: Missing setReferencedColumn, removeReferencedColumn

        template = template.replace("@@model@@", model.name) \
                   .replace("@@formattedCols@@", formatted_columns)\
                   .replace("@@appNameUpper@@", self.app.name) \
                   .replace("@@refFields@@", ref_fields_str) \
                   .replace("@@dbDriverName@@", db_driver_name)\
                   .replace("@@refConstructorFields@@", ref_constructor_fields) \
                   .replace("@@refFromDBFields@@", ref_from_db) \
                   .replace("@@baseConstructorFields@@", base_constructor_fields) \
                   .replace("@@package@@", self.app.package) \
                   .replace("@@queryName@@", self.query_name) \
                   .replace("@@fullResponseBuild@@", full_response_build) \
                   .replace("@@baseModelFieldCount@@", str(len(model.columns)))

        write(model_path, template)
