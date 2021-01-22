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
* [X] two players can have the same nickname in the Database (remove unique index). If a premium user logs in with a cracked nickname, unregister cracked user. When checking for different namecase, ignore "invalid" accounts.
* [X] add thread limit
* [X] global /msg
* [X] MOTD System (or use MiniMOTD/PistonMOTD)
* [ ] add queue to retry failed queries
* [ ] stafflist
* [ ] maintenance (proxy instance)
* [ ] maintenance (global)
* [ ] AntiVPN
* [ ] sub-servers managers: add/remove/edit servers
* [ ] Auto announcer

# TODO Commands

* Command framework
* Auth
    * [X] clear cache from user (in-memory and redis -> invoke pubsub event)
    * [ ] register other
    * [ ] unregister other
    * [ ] changepassword other
* Essentials (with cross-proxy support!)
    * [ ] send
    * [ ] warn when proxy-name is empty/null
