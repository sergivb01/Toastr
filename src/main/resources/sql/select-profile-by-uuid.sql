SELECT UUID_FROM_BIN(uuid) AS uuid, username, account_type, INET_NTOA(first_address) AS first_address, INET_NTOA(last_address) AS last_address, first_login, last_login, password, salt
    FROM playerdata
    WHERE uuid = UUID_TO_BIN(?);