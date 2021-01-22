# Toastr

Should probably make a good description

* [ ] Replace "return null" to Optional

# Creation
```mysql
CREATE TABLE playerdata
(
    uuid              char(36) character set ascii  NOT NULL,
    player_name       varchar(16)                   NOT NULL,
    player_name_lower varchar(16)                   NOT NULL,
    first_address     int unsigned                  NOT NULL comment 'ipv4 address stored as an unsigned int',
    last_address      int unsigned                  NOT NULL comment 'ipv4 address stored as an unsigned int',
    first_login       timestamp                     NOT NULL,
    last_login        timestamp                     NOT NULL,
    password          varchar(256)                  NULL,
    salt              varchar(10)                   NULL,
    account_type      varchar(25) default 'PREMIUM' NOT NULL,
    CONSTRAINT playerdata_uuid_uindex
        UNIQUE (uuid)
) COMMENT 'describes the essential data from a player';

CREATE INDEX playerdata_username_index ON playerdata (player_name);

CREATE INDEX playerdata_usernamelower_index ON playerdata (player_name_lower);

ALTER TABLE playerdata ADD PRIMARY KEY (uuid);
```

# Migration
```mysql
INSERT INTO sergi_toastr.playerdata (uuid, player_name, player_name_lower, first_address, last_address, first_login, last_login, password, salt, account_type)
    SELECT old.uuid, old.name, lower(old.name), INET_ATON(old.reg_ip), INET_ATON(old.log_ip), old.firstjoin, old.lastjoin, old.password, old.salt,
    CASE
    	WHEN old.premium = 1 THEN "PREMIUM"
    	ELSE "CRACKED"
    END
    FROM dynamic_auth.playerdata old;
```

# TODO Features/commands

* [X] migrate to pidgin
* [X] two players can have the same nickname in the Database (remove unique index). If a premium user logs in with a cracked nickname, unregister cracked user. When checking for different namecase, ignore "invalid" accounts.
* [X] add thread limit
* [X] global /msg
* [X] MOTD System (or use MiniMOTD/PistonMOTD)
* [ ] add queue to retry failed queries
* [ ] check for auto-reconnect in Redis and MySQL
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
