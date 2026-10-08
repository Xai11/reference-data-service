CREATE TABLE dictionary_attribute (
    id                BIGSERIAL PRIMARY KEY,
    dictionary_id     BIGINT       NOT NULL REFERENCES dictionary (id) ON DELETE CASCADE,
    code              VARCHAR(100) NOT NULL,
    name              VARCHAR(255) NOT NULL,
    type              VARCHAR(20)  NOT NULL,
    required          BOOLEAN      NOT NULL DEFAULT FALSE,
    sort_order        INTEGER      NOT NULL DEFAULT 0,
    ref_dictionary_id BIGINT       REFERENCES dictionary (id),
    CONSTRAINT uq_dictionary_attribute_code UNIQUE (dictionary_id, code),
    CONSTRAINT ck_dictionary_attribute_ref
        CHECK ((type = 'REFERENCE') = (ref_dictionary_id IS NOT NULL))
);

CREATE TABLE dictionary_item_value (
    id            BIGSERIAL PRIMARY KEY,
    item_id       BIGINT NOT NULL REFERENCES dictionary_item (id) ON DELETE CASCADE,
    attribute_id  BIGINT NOT NULL REFERENCES dictionary_attribute (id) ON DELETE CASCADE,
    value_string  VARCHAR(2000),
    value_number  NUMERIC(19, 6),
    value_boolean BOOLEAN,
    value_date    DATE,
    value_item_id BIGINT REFERENCES dictionary_item (id),
    CONSTRAINT uq_dictionary_item_value UNIQUE (item_id, attribute_id)
);

CREATE INDEX idx_item_value_attribute ON dictionary_item_value (attribute_id);
CREATE INDEX idx_item_value_ref_item ON dictionary_item_value (value_item_id);
