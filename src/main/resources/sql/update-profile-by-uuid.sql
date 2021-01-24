UPDATE playerdata
    SET username = ?, last_address = INET_ATON(?), last_login = ?, password = ?, salt = ?
    WHERE uuid = ?;