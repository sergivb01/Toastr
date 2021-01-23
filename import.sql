INSERT INTO sergi_toastr.playerdata (uuid, player_name, player_name_lower, first_address, last_address, first_login, last_login, password, salt, account_type)
SELECT old.uuid, old.name, lower(old.name), INET_ATON(old.reg_ip), INET_ATON(old.log_ip), old.firstjoin, old.lastjoin, old.password, old.salt,
       CASE
           WHEN old.premium = 1 THEN "PREMIUM"
           ELSE "CRACKED"
           END
FROM dynamic_auth.playerdata old;