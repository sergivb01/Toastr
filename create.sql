CREATE TABLE playerdata
(
    uuid           binary(16)                 NOT NULL,
    username       varchar(16)                NOT NULL,
    username_lower varchar(16) GENERATED ALWAYS AS (lower(username)) STORED,
    first_address  int unsigned               NOT NULL comment 'ipv4 address stored as an unsigned int',
    last_address   int unsigned               NOT NULL comment 'ipv4 address stored as an unsigned int',
    first_login    timestamp                  NOT NULL DEFAULT current_timestamp(),
    last_login     timestamp                  NOT NULL DEFAULT current_timestamp(),
    password       varchar(256)               NULL,
    salt           varchar(10)                NULL,
    account_type   ENUM ('PREMIUM','CRACKED') NOT NULL,
    CONSTRAINT playerdata_uuid_uindex
        UNIQUE (uuid)
) COMMENT 'describes the essential data from a player';

CREATE INDEX playerdata_username_index ON playerdata (username);

CREATE INDEX playerdata_usernamelower_index ON playerdata (username_lower);

ALTER TABLE playerdata
    ADD PRIMARY KEY (uuid);


DELIMITER $$

CREATE TRIGGER before_playerdata_update
    BEFORE UPDATE
    ON playerdata
    FOR EACH ROW
BEGIN
    SET NEW.account_type = upper(NEW.account_type);

    IF (NEW.account_type != 'PREMIUM' AND NEW.account_type != 'CRACKED') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Invalid account_type';
    END IF;
END$$

CREATE TRIGGER before_playerdata_insert
    BEFORE INSERT
    ON playerdata
    FOR EACH ROW
BEGIN
    SET NEW.account_type = upper(NEW.account_type);

    IF (NEW.account_type != 'PREMIUM' AND NEW.account_type != 'CRACKED') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Invalid account_type';
    END IF;
END$$

CREATE FUNCTION UUID_TO_BIN(_uuid BINARY(36))
    RETURNS BINARY(16)
    LANGUAGE SQL DETERMINISTIC
    CONTAINS SQL SQL SECURITY INVOKER
    RETURN
        UNHEX(CONCAT(
                SUBSTR(_uuid, 15, 4),
                SUBSTR(_uuid, 10, 4),
                SUBSTR(_uuid, 1, 8),
                SUBSTR(_uuid, 20, 4),
                SUBSTR(_uuid, 25)));
$$

CREATE FUNCTION UUID_FROM_BIN(_bin BINARY(16))
    RETURNS BINARY(36)
    LANGUAGE SQL DETERMINISTIC
    CONTAINS SQL SQL SECURITY INVOKER
    RETURN
        LCASE(CONCAT_WS('-',
                        HEX(SUBSTR(_bin, 5, 4)),
                        HEX(SUBSTR(_bin, 3, 2)),
                        HEX(SUBSTR(_bin, 1, 2)),
                        HEX(SUBSTR(_bin, 9, 2)),
                        HEX(SUBSTR(_bin, 11))
            ));
$$

DELIMITER ;