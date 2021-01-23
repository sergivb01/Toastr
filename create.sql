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

ALTER TABLE playerdata
    ADD PRIMARY KEY (uuid);