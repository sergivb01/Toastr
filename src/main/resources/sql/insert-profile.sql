INSERT IGNORE INTO playerdata
    (uuid, username, account_type, first_address, last_address, first_login, last_login, password, salt)
    VALUES (?, ?, ?, INET_ATON(?), INET_ATON(?), ?, ?, ?, ?);