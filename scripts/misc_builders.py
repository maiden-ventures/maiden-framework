from helpers import *
import shutil
import os

def build_boot(app):
    template = read_template("boot")
    app_name = app.name
    package = app.package

    out = template.replace("@@package@@", package)\
                  .replace("@@appNameUpper@@", app_name)

    file_name = os.path.join(app.base_path, "Boot.scala")
    write(file_name, out)

def build_api_service(app):
    #handle the automatically generated model APIs
    apis = ["%sApi.%sApi" % (m.name, m.name_lower) for m in app.models if m.generate_api]

    #now handle any APIs that are outside of our scope

    for custom_api in app.custom_apis:
        apis +=  ["%sApi.%sApi" % (camelize(custom_api['name']), camelize(custom_api['name'], False))]

    apis = " :+: ".join(apis)

    service = read_template("api-service")
    out = service.replace("@@package@@", app.package)\
                 .replace("@@app@@", app.name)\
                 .replace("@@appLower@@", app.name_lower)\
                 .replace("@@api_list@@", apis)
    file_name = os.path.join(app.base_path, "%sApi.scala" % (app.name))

    write(file_name, out)


def build_logback(app):
    template = read_template("logback")
    file_name = os.path.join(app.source_directory, "config/logback.xml")

    out = template.replace("@@appName@@", app.name_lower)

    write(file_name, out)

def build_app_ini(app):
    template = read_template("application.ini")
    file_name = os.path.join(app.source_directory, "config/launcher.conf")
    out = template.replace("@@httpInterface@@", ":%s" % (app.port)) \
                  .replace("@@certificatePath@@", app.certificate_path) \
                  .replace("@@keyPath@@", app.key_path) \
                  .replace("@@maxRequestSize@@", app.max_request_size) \
                  .replace("@@appNameLower@@", app.name_lower)

    if app.https_port != "":
      out = out.replace("@@httpsInterface@@", ":%s" % self.https_port)
    else:
        out = out.replace("@@httpsInterface@@", "")

    write(file_name, out, app.prompt_overwrite)

def copy_tools(app):
    base = os.path.join(app.source_directory, "tools")

    if not os.path.exists(base):
        os.mkdir(base)
    shutil.copyfile(os.path.join(BASE_ROOT_PATH, "tools/zipkin.jar"), os.path.join(base, "zipkin.jar"))

def security_info(app_config):
    sec = {}

    #read in security (if it exsists)
    if 'security' in app_config:
        base_sec = app_config['security']
        if base_sec['method'] == "token":
            sec['security_import'] ="import maiden.auth.token.TokenAuth._"
            access_token = base_sec['access_token']
            if 'param_name' in base_sec:
                param_name = base_sec['param_name']
            else:
                param_name = "access_token"

            if 'header_name' in base_sec:
                header_name = base_sec['header_name']
            else:
                header_name = "X-ACCESS-TOKEN"


            sec['security_config'] = """
app.security.param_name="%s"
app.security.header_name="%s"
app.security.access_token="%s"
            """ % (param_name, header_name, access_token)
        #add more here
    else:
        sec['security_import'] = "import maiden.auth.anon.AnonAuth._"
        sec['security_config'] = ""

    return sec
