UPDATE playerdata
    SET player_name = ?, last_address = INET_ATON(?), last_login = ?, password = ?, salt = ?
    WHERE uuid = ?;