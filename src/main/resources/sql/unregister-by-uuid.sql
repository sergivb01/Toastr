UPDATE playerdata
    SET password = NULL, salt = NULL
    WHERE uuid = ?;