
#some default models to add if the user selects
#per-user authentication
DEFAULT_USER_MODEL = {
    "name": "User",
    "db_name": "users",
    "columns": [
        {"name": "user_name", "type": "varchar", "limit": 64,"index": True,
         "validations": [{"longer_than": 3}, {"shorter_than":"64"}]},
        {"name": "access_token", "type": "varchar", "limit": 128, "index": True,
         "validations": [{"longer_than": 10}, {"shorter_than":"129"}]},
    ]
}

DEFAULT_SOCIAL_USER_MODEL = {
    "name": "SocialUser",
    "columns": [
        {"name": "user_id", "type": "bigint","index": True},
        {"name": "provider", "type": "varchar", "limit": 100, "index": True},
        {"name": "uid", "type": "varchar", "limit": 255, "index": True},
        {"name": "access_token", "type": "varchar", "limit": 1024, "index": True},
        {"name": "secret_key", "type": "varchar", "limit": 1024, "nullable": True, "index": True},
        {"name": "extra", "type": "varchar", "limit": 8192, "nullable": True}
    ]
}
