from helpers import *


class ApiBuilder:

    def __init__(self, app):
        self.template = read_template("api")
        self.app = app


        self.security_import = app.security["security_import"]
        self.build()


    def add_validations(self, c, name=None): # scala_type, validations, name = None):

        def handle_escape(v):
            try:
                int(v)
                return v
            except:
                try:
                    float(v)
                    return v
                except:
                  return '"%s"' % (v)

        vstr = ""

        #verify not null if specified
        if not c.nullable:
            if c.scala_type == "String":
                if "non_empty_string" not in c.validations:
                    c.validations.insert(0, "non_empty_string")

            elif c.scala_type in ("LocalDateTime", "LocalDate"):
                if "non_empty_datetime" not in c.validations:
                    c.validations.insert(0, "non_empty_datetime")
            elif c.scala_type != "LocalDateTime" and c.scala_type != "Boolean":
                if "non_empty_numeric" not in c.validations:
                    c.validations.insert(0, "non_empty_numeric")

        if len(c.validations) > 0:
            if name:
              vstr += '.should("' + name + '")(%s)'
            else:
              vstr += '.should("' + c.name + '")(%s)'

            vals = []
            for v in c.validations:
                if type(v) == type("") and c.scala_type not in ("LocalDateTime", "DateTime", "String"):
                    vals.append("%s[%s]" % (v, c.scala_type))

                elif type(v) == type("") and c.scala_type in ("LocalDateTime", "DateTime"):
                    vals.append("%s" % (v))
                elif type(v) == type("") and c.scala_type == "String":
                    vals.append("%s" % (v))
                elif type(v) == type({}):
                    for (key, value) in v.items():
                      value = str(value).split(" ")
                      if c.scala_type != "String":
                        vals.append("%s[%s](%s)" % (key, c.scala_type, ",".join([handle_escape(x) for x in value])))
                      else:
                        vals.append("%s(%s)" % (key, ",".join([handle_escape(x) for x in value])))

            vals = " and ".join([v for v in vals])
            return vstr % (vals)

        else:
            return vstr

    def build(self):
        for model in self.app.models:

            #Endpoint params
            param_str = """param("%s").as[%s]%s"""

            all_model_cols = ["%s: %s" % (c.name, c.db_type) for c in model.columns]
            all_model_cols_str = ", ".join(all_model_cols)

            cols = [c for c in model.columns if c.name not in ("id", "createdAt", "updatedAt")]

            #for creating the body Decoder
            core_field_comprehension_mappings = "\n".join(['%s <- c.downField("%s").as[Option[%s]]' % (c.name, c.name, c.scala_type) for c in model.columns if c.name not in ("id", "createdAt", "updatedAt")])


            cfields = []

            #don't do validations at parsing time
            for cf in cols:
                #if len(cf.formatters) > 0:
                    #add the formatters
                #    cfields.append("%s.%s" % (cf.name, ".".join(cf.formatters)))
                #else:
                cfields.append(cf.name)

            core_field_mappings = ",".join(cfields)


            create_param_args = " :: ".join(['param("%s").as[%s]%s' % (c.name, c.scala_type, self.add_validations(c)) for c in model.columns])
            create_params = ", ".join(["%s: %s" % (c.name, c.scala_type) for c in cols])
            model_creation_args = ", ".join([c.name for c in cols])

            param_list = ", ".join(["%s: Option[%s]" % (c.name, c.scala_type) for c in cols])
            update_params = ", ".join([c.name for c in cols])

            #user.username.should("username")(max_length(50)),

            validations = ",\n".join(["%s.%s%s" % (model.name_lower, c.name, self.add_validations(c)) for c in cols if c.validations != []])
                                     # or c.nullable == False])


            out = self.template.replace("@@model@@", model.name)\
                               .replace("@@lowerCaseModel@@", model.name_lower) \
                               .replace("@@package@@", self.app.package)\
                               .replace("@@securityImport@@", self.security_import)\
                               .replace("@@coreFieldComprehensionMappings@@", core_field_comprehension_mappings) \
                               .replace("@@coreFieldMappings@@", core_field_mappings) \
                               .replace("@@createParamArgs@@", create_param_args)\
                               .replace("@@createParams@@", create_params)\
                               .replace("@@modelCreationArgs@@", model_creation_args)\
                               .replace("@@modelColumns@@", all_model_cols_str)\
                               .replace("@@validations@@", validations) \
                               .replace("@@paramList@@", param_list).replace("@@updateParams@@", update_params)

            write(os.path.join(self.app.base_path, "components/%s/%sApi.scala" % (underscore(model.name), model.name)), out)
