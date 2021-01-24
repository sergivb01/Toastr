UPDATE playerdata
    SET password = NULL, salt = NULL
    WHERE username_lower = lower(?) AND account_type = 'CRACKED'
    ORDER BY last_login DESC
    LIMIT 1;