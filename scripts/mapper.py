
DB_MAP= {
    "varchar": {"db": "varchar", "scala":"String", "validations": []},
    "bigint": {"db": "bigint", "scala": "Long", "validations": []},
    "int": {"db": "int", "scala": "Int", "validations": []},
    "datetime": {"db": "timestamp", "scala": "LocalDateTime", "validations": []},
    "timestamp": {"db": "timestamp", "scala": "LocalDateTime", "validations": []},
    "boolean": {"db": "boolean", "scala": "Boolean", "validations": []},

    "email": {"db": "varchar", "scala":"String", "validations": ['email']},
    "phone": {"db": "varchar", "scala":"String", "validations": [{'phone': "US"}],
              "formatters": ["phone"]},

    "url": {"db": "varchar", "scala":"String", "validations": ['url']},
    "ip_address": {"db": "varchar", "scala": "String", "validations": ["ip_adress"]},
    "social_security": {"db": "varchar", "scala":"String", "validations": ['social_security'],
                        "formatters": ["social_security"]},
    "postal_code": {"db": "varchar", "scala":"String", "validations": [{'postal_code': "US"}],
                    "formatters": ["postal_code"]},
    "json": {"db": "varchar", "scala":"String", "validations": ['json']},
    "xml": {"db": "varchar", "scala":"String", "validations": ['xml']},
    "list": {"db": "varchar", "scala":"String", "validations": ['xml'],
             "formatters": ["list"]},
    "map": {"db": "varchar", "scala":"String", "validations": ['xml']},
  }
