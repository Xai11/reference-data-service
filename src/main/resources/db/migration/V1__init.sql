CREATE TABLE dictionary (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(100)  NOT NULL UNIQUE,
    name        VARCHAR(255)  NOT NULL,
    description VARCHAR(1000),
    active      BOOLEAN       NOT NULL DEFAULT TRUE
);

CREATE TABLE dictionary_item (
    id            BIGSERIAL PRIMARY KEY,
    dictionary_id BIGINT        NOT NULL REFERENCES dictionary (id) ON DELETE CASCADE,
    code          VARCHAR(100)  NOT NULL,
    name          VARCHAR(255)  NOT NULL,
    description   VARCHAR(1000),
    sort_order    INTEGER       NOT NULL DEFAULT 0,
    active        BOOLEAN       NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_dictionary_item_code UNIQUE (dictionary_id, code)
);
