from helpers import *

class EncoderBuilder:

    def __init__(self, app):
        self.app = app
        self.template = read_template("encoder")

        self.encoders = []
        self.encoder_dir = os.path.join(self.app.base_path, "encoders")

        self.build()

    def build(self):
        payload_key = self.app.payload_key

        package_name = self.app.package

        for model in [m for m in self.app.models if m.generate_model]:
            model_name = model.name
            lower_model_name = model.name_lower
            fields = [x.name for x in model.columns]

            #json_fields = ['"%s" -> m.%s' % (underscore(x), camelize(x, False)) for x in fields]
            json_fields = ['"%s" -> m.%s' % (f.name, f.name) for f in model.columns]

            json_fields_str = ", ".join(json_fields)

            out = self.template\
                      .replace("@@package@@", package_name)\
                      .replace("@@model@@", model_name)\
                      .replace("@@lowerCaseModel@@", lower_model_name)\
                      .replace("@@jsonFields@@", json_fields_str)\
                      .replace("@@payloadKey@@", payload_key)

            file_name = os.path.join(self.app.base_path, "components/%s/%sEncoder.scala" % (underscore(model.name), model_name))

            write(file_name, out)
            self.encoders.append("%sResponseEncoders" % (model_name))

        #now write out the master response encoders file
        encoder_traits = ' '.join(["with %s" % (x) for x in self.encoders])
        enc = read_template("response-encoders")

        app_name = self.app.name
        package = self.app.package
        out = enc\
              .replace("@@package@@", package)\
              .replace("@@response_encoders@@", encoder_traits)

        file_name = os.path.join(self.app.base_path, "ResponseEncoders.scala")
        write(file_name, out)
