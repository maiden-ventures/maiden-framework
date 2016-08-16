
DB_MAP= {
    "varchar": {"db": "varchar", "scala":"String", "validations": []},
    "text": {"db": "text", "scala":"String", "validations": []},
    "bigint": {"db": "bigint", "scala": "Long", "validations": []},
    "int": {"db": "integer", "scala": "Int", "validations": []},
    "datetime": {"db": "timestamp", "scala": "LocalDateTime", "validations": []},
    "date": {"db": "timestamp", "scala": "LocalDateTime", "validations": []},
    "timestamp": {"db": "timestamp", "scala": "LocalDateTime", "validations": []},
    "boolean": {"db": "boolean", "scala": "Boolean", "validations": []},
    "money": {"db": "decimal", "scala": "BigDecimal", "validations" :["money"]},
    "email": {"db": "varchar", "scala":"String", "validations": ['email']},
    "phone": {"db": "varchar", "scala":"String", "validations": [{'phone': "US"}],
              "formatters": ["phone"]},

    "url": {"db": "varchar", "scala":"String", "validations": ['url']},
    "ip_address": {"db": "varchar", "scala": "String", "validations": ["ip_adress"]},
    "social_security": {"db": "varchar", "scala":"String", "validations": ['social_security'],
                        "formatters": ["social_security"]},
    "postal_code": {"db": "varchar", "scala": "String", "validations": [{'postal_code': "US"}]},
    "country_code": {"db": "varchar", "scala": "String", "validations": ["country_code", {"min_length": "3", "max_length": "3"}],
       "formatters": ["postal_code"]},
    "json": {"db": "varchar", "scala":"String", "validations": ['json']},
    "xml": {"db": "varchar", "scala":"String", "validations": ['xml']},
    "list": {"db": "varchar", "scala":"String", "validations": ['xml'],
             "formatters": ["list"]},
    "map": {"db": "varchar", "scala":"String", "validations": ['xml']}
  }
