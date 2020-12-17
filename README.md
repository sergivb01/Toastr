# Toastr

Should probably make a good description

* [ ] Replace "return null" to Optional

# Migration
```mysql
INSERT INTO sergi_toastr.playerdata (uuid, player_name, player_name_lower, first_address, last_address, first_login, last_login, password, salt, account_type)
    SELECT old.uuid, old.name, lower(old.name), INET_ATON(old.reg_ip), INET_ATON(old.log_ip), old.firstjoin, old.lastjoin, old.password, old.salt,
    CASE
    	WHEN old.premium = 1 THEN "PREMIUM"
    	ELSE "CRACKED"
    END
    FROM water_auth.playerdata old
    LIMIT 10000;
```

# TODO Features/commands

* [X] migrate to pidgin
* [ ] global /msg
* [ ] stafflist
* [ ] maintenance (proxy instance)
* [ ] maintenance (global)
* [ ] MOTD System (or use MiniMOTD/PistonMOTD)
* [ ] AntiVPN
* [ ] sub-servers managers: add/remove/edit servers
* [ ] Auto announcer

# TODO Commands

* Command framework
* Auth
    * [ ] register other
    * [ ] unregister other
    * [ ] changepassword other
* Essentials (with cross-proxy support!)
    * [ ] send