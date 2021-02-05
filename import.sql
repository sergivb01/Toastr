INSERT IGNORE INTO toastr.playerdata (uuid, username, first_address, last_address, first_login, last_login, password, salt, account_type)
    SELECT UUID_TO_BIN(old.uuid), old.name, INET_ATON(old.reg_ip), INET_ATON(old.log_ip), old.firstjoin, old.lastjoin, old.password, old.salt,
       CASE
           WHEN old.premium = 1 THEN "PREMIUM"
           ELSE "CRACKED"
           END
FROM water_auth.playerdata old;