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
    print files
    for d in files:
        #subprocess.call(["java", "-jar", os.path.join(BASE_PATH, "../tools/scalariform.jar"), "+preserveDanglingCloseParenthesis", "--recurse", d], stdout=subprocess.PIPE)
        subprocess.call(["java", "-jar", os.path.join(BASE_PATH, "../tools/scalafmt.jar"), "-i", "-s", "defaultWithAlign", "--alignByArrowEnumeratorGenerator", "true", "--continuationIndentDefnSite","2", "--continuationIndentCallSite", "2", "--bestEffortInDeeplyNestedCode", "--maxColumn", "80", "-f", d], stdout=subprocess.PIPE)
        #subprocess.call(["java", "-jar", os.path.join(BASE_PATH, "../tools/scalafmt.jar"), "-i", "-s", "default", "--maxColumn", "80", "-f", d], stdout=subprocess.PIPE)

def confirm(prompt=None, resp=False):
    """prompts for yes or no response from the user. Returns True for yes and
    False for no.

    'resp' should be set to the default value assumed by the caller when
    user simply types ENTER.

    >>> confirm(prompt='Create Directory?', resp=True)
    Create Directory? [y]|n:
    True
    >>> confirm(prompt='Create Directory?', resp=False)
    Create Directory? [n]|y:
    False
    >>> confirm(prompt='Create Directory?', resp=False)
    Create Directory? [n]|y: y
    True

    """

    if prompt is None:
        prompt = 'Confirm'

    if resp:
        prompt = '%s [%s]|%s: ' % (prompt, 'y', 'n')
    else:
        prompt = '%s [%s]|%s: ' % (prompt, 'n', 'y')

    while True:
        ans = raw_input(prompt)
        if not ans:
            return resp
        if ans not in ['y', 'Y', 'n', 'N']:
            print 'please enter y or n.'
            continue
        if ans == 'y' or ans == 'Y':
            return True
        if ans == 'n' or ans == 'N':
            return False

def write(name, content, prompt_overwrite=False):
    #verify that the base path exists
    base = os.path.dirname(name)
    if not os.path.exists(base):
        os.makedirs(base)

    if os.path.exists(name) and prompt_overwrite:
        #prompt for confirmation
        if confirm("Overwrite %s?" % (name)):
            fd = open(name, "w+")
            fd.write(content)
            fd.close()
    else:
        fd = open(name, "w+")
        fd.write(content)
        fd.close()

def optionize(c):
    if c.nullable:
        return "Option[%s]" % (c.scala_type)
    else:
        return c.scala_type

def optionize_from_vals(nullable, scala_type):
    if nullable:
        return "Option[%s]" % (scala_type)
    else:
        return scala_type
