INSERT IGNORE INTO playerdata
    (uuid, player_name, player_name_lower, account_type, first_address, last_address, first_login, last_login, password, salt)
    VALUES (?, ?, lower(?), ?, INET_ATON(?), INET_ATON(?), ?, ?, ?, ?);