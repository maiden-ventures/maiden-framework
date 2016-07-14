from helpers import *


class ApiBuilder:

    def __init__(self, app):
        self.template = read_template("api")
        self.api_dir = os.path.join(app.base_path, "api")
        self.app = app


        self.security_import = app.security["security_import"]
        self.build()


    def add_validations(self,  scala_type, validations, name = None):

        def handle_escape(v):
            try:
                int(v)
                return v
            except:
                return '"%s"' % (v)

        if len(validations) > 0:
            if name:
              vstr = '.should("' + name + '")(%s)'
            else:
                vstr = ".should(%s)"

            vals = []
            for v in validations:
                if (type(v) == type("")):
                    vals.append("%s[%s]" % (v, scala_type))
                elif type(v) == type({}):
                    for (key, value) in v.items():
                      value = str(value).split(" ")
                      if scala_type != "String":
                        vals.append("%s[%s](%s)" % (key, scala_type, ",".join([handle_escape(x) for x in value])))
                      else:
                        vals.append("%s(%s)" % (key, ",".join([handle_escape(x) for x in value])))

            vals = " and ".join([v for v in vals])
            return vstr % (vals)

        else:
            return ""

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
            for cf in cols:
                if len(cf.formatters) > 0:
                    #add the formatters
                    cfields.append("%s.%s" % (cf.name, ".".join(cf.formatters)))
                else:
                    cfields.append(cf.name)

            core_field_mappings = ",".join(cfields)


            create_param_args = " :: ".join(['param("%s").as[%s]%s' % (c.name, c.scala_type, self.add_validations(c.scala_type, c.validations)) for c in model.columns])
            create_params = ", ".join(["%s: %s" % (c.name, c.scala_type) for c in cols])
            model_creation_args = ", ".join([c.name for c in cols])

            param_list = ", ".join(["%s: Option[%s]" % (c.name, c.scala_type) for c in cols])
            update_params = ", ".join([c.name for c in cols])

            validations = ",\n".join(["%s.%s%s" % (model.name_lower, c.name, self.add_validations(c.scala_type, c.validations)) for c in cols if c.validations != []])


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

            write(os.path.join(self.api_dir, "%s.scala" % (model.name)), out)
