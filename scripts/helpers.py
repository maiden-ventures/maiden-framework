import os
import inflection
import shutil
import subprocess
from mapper import DB_MAP


BASE_PATH = os.path.realpath(os.path.dirname(__file__))
BASE_ROOT_PATH = os.path.abspath(os.path.join(BASE_PATH, ".."))

def camelize(s, capitalize=True):
    return inflection.camelize(s, capitalize)

def underscore(s):
    return inflection.underscore(s).lower()

def make_dir(d):
    if not os.path.exists(d):
        os.makedirs(d)

def exists_and_true(data, key):
    if key in data and data[key]: return True
    else: return False


def db_type(t): return DB_MAP[t]["db"]
def scala_type(t): return DB_MAP[t]["scala"]
def get_validations(t): return DB_MAP[t].get("validations", [])
def get_formatters(t): return DB_MAP[t].get("formatters", [])

def key_with_default(data, key, default):
    if key in data and data[key]: return data[key]
    if key not in data: return default

def read_template(name):
    fd = open(os.path.join(BASE_PATH, "code-templates", name))
    contents = fd.read()
    fd.close()
    return contents

def format_scala(files):
    for d in files:
        subprocess.call(["java", "-jar", os.path.join(BASE_PATH, "../tools/scalafmt.jar"), "-i", "-s", "default", "--alignByArrowEnumeratorGenerator", "true", "--continuationIndentDefnSite","2", "--continuationIndentCallSite", "2", "--maxColumn", "80", "-f", d], stdout=subprocess.PIPE)
        #subprocess.call(["java", "-jar", os.path.join(BASE_PATH, "../tools/scalariform.jar"), "+preserveDanglingCloseParenthesis", "--recurse", d], stdout=subprocess.PIPE)

def write(name, content):
    fd = open(name, "w+")
    fd.write(content)
    fd.close()
