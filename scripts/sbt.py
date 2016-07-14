from helpers import *

class SbtBuilder:

    def __init__(self, app):

        self.app = app
        self.sbt_template = read_template("sbt")
        self.plugins_template =  read_template("sbt-plugins")

        self.build_sbt()
        self.build_plugins()


    def build_sbt(self):
        self.sbt_template = self.sbt_template\
                                .replace("@@namespace@@", self.app.namespace)\
                                .replace("@@appName@@", self.app.name_lower)
        write(os.path.join(self.app.source_directory, "build.sbt"), self.sbt_template)

    def build_plugins(self):
        write(os.path.join(self.app.source_directory, "project", "plugins.sbt"),
              self.plugins_template)
