UPDATE playerdata
    SET player_name = ?, player_name_lower = lower(?), last_address = INET_ATON(?), last_login = ?, password = ?, salt = ?
    WHERE uuid = ?;