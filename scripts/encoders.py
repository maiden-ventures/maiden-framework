from helpers import *

class EncoderBuilder:

    def __init__(self, models, config):
        self.template = read_template("encoder")

        self.config = config
        self.models = models

        self.encoders = []

        self.encoder_dir = os.path.join(self.config['base_path'], "encoders")

        self.build()

    def build(self):
        if 'payload_key' in self.config['app']:
          payload_key = self.config['app']['payload_key']
        else:
            payload_key = ""

        package_name = self.config['package']

        for model in self.models:
            model_name = camelize(model['name'])
            lower_model_name = camelize(model['name'], False)
            fields = [x['name'] for x in model['columns']]

            json_fields = ['"%s" -> m.%s' % (underscore(x), camelize(x, False)) for x in fields]

            json_fields_str = ", ".join(json_fields)

            out = self.template\
                      .replace("@@package@@", package_name)\
                      .replace("@@model@@", model_name)\
                      .replace("@@lowerCaseModel@@", lower_model_name)\
                      .replace("@@jsonFields@@", json_fields_str)\
                      .replace("@@payloadKey@@", payload_key)

            file_name = os.path.join(self.encoder_dir, "%s.scala" % (model_name))

            write(file_name, out)
            self.encoders.append("%sResponseEncoders" % (model_name))

        #now write out the master response encoders file
        encoder_traits = ' '.join(["with %s" % (x) for x in self.encoders])
        enc = read_template("response-encoders")

        app_name = self.config['app']['name']
        package = self.config['package']
        out = enc\
              .replace("@@package@@", package)\
              .replace("@@response_encoders@@", encoder_traits)

        file_name = os.path.join(self.config['base_path'], "ResponseEncoders.scala")
        write(file_name, out)
