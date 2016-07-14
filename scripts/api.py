from helpers import *


class ApiBuilder:

    def __init__(self, app):
        self.template = read_template("api")
        self.api_dir = os.path.join(app.base_path, "api")


        self.security_import = app.security["security_import"]
        self.build()


    def add_validations(self,  col_type, validations, name = None):

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
                    vals.append("%s[%s]" % (v, col_type))
                elif type(v) == type({}):
                    for (key, value) in v.items():
                      value = str(value).split(" ")
                      if col_type != "String":
                        vals.append("%s[%s](%s)" % (key, col_type, ",".join([handle_escape(x) for x in value])))
                      else:
                        vals.append("%s(%s)" % (key, ",".join([handle_escape(x) for x in value])))

            vals = " and ".join([v for v in vals])
            return vstr % (vals)

        else:
            return ""

    def build(self):
        for model in app.models:

            #Endpoint params
            param_str = """param("%s").as[%s]%s"""

            all_model_cols = ["%s: %s" % (c.name, c.db_type) for c in model.columns]
            all_model_cols_str = ", ".join(all_model_cols)

            cols = [c for c in model.columns if c.name not in ("id", "createdAt", "updatedAt")]

            #for creating the body Decoder
            core_field_comprehension_mappings = "\n".join(['%s <- c.downField("%s").as[Option[%s]]' % (c.name, c.name, c.scala_type) for c in model.columns])


            cfields = []
            for cf in model.columns:
                if len(cf.formatters) > 0:
                    #add the formatters
                    cfields.append("%s.%s" % (cf.name, ".".join(cf.formatters)))
                else:
                    cf.fields.append(cf.name)

            core_field_mappings = ",".join(cfields)


            create_param_args = " :: ".join(['param("%s").as[%s]%s' % (c[0], c[1], self.add_validations(c.validations)) for c in model.columns])
            create_params = ", ".join(["%s: %s" % (c.name, c.scala_type) for c in cols])
            model_creation_args = ", ".join([c.name for c in cols])

            param_list = ", ".join(["%s: Option[%s]" % (c.name, c.scala_type) for c in cols])
            update_params = ", ".join([c.name for c in cols])

            validations = ",\n".join(["%s.%s%s" % (model.name_lower, c.name, self.add_validations(c.validations)) for c in cols if c.validations != []])


            out = self.template.replace("@@model@@", camelize(model['name'], True))\
                               .replace("@@lowerCaseModel@@", camelize(model['name'],False)) \
                               .replace("@@package@@", self.config['package'])\
                               .replace("@@securityImport@@", self.security_import)\
                               .replace("@@coreFieldComprehensionMappings@@", core_field_comprehension_mappings) \
                               .replace("@@coreFieldMappings@@", core_field_mappings) \
                               .replace("@@createArgs@@", create_args)\
                               .replace("@@createParamArgs@@", create_param_args)\
                               .replace("@@createParams@@", create_params)\
                               .replace("@@modelCreationArgs@@", model_creation_args)\
                               .replace("@@modelColumns@@", all_model_cols_str)\
                               .replace("@@optionalParams@@", optional_params)\
                               .replace("@@validations@@", validations) \
                               .replace("@@paramList@@", param_list).replace("@@updateParams@@", update_params)

            write(os.path.join(self.api_dir, "%s.scala" % (camelize(model['name']))), out)
