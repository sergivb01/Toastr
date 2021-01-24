SELECT uuid, player_name, account_type, first_address, last_address, first_login, last_login, password, salt
    FROM playerdata
    WHERE player_name_lower = lower(?) AND account_type = 'CRACKED';