SELECT UUID_FROM_BIN(uuid) AS uuid, username, account_type, first_address, last_address, first_login, last_login, password, salt
    FROM playerdata
    WHERE username_lower = lower(?);